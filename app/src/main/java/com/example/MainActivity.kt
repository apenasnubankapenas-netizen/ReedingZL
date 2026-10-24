package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.FullScreenSubtitleView
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReelsBookPagerScreen
import com.example.ui.theme.SrtTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.setReelsMode(true)
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val projects by viewModel.allProjects.collectAsStateWithLifecycle(initialValue = emptyList())
            val folders by viewModel.allFolders.collectAsStateWithLifecycle(initialValue = emptyList())
            val pdfBooks by viewModel.allPdfBooks.collectAsStateWithLifecycle(initialValue = emptyList())
            val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

            val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
            val cues by viewModel.cues.collectAsStateWithLifecycle()
            val secondaryCues by viewModel.secondaryCues.collectAsStateWithLifecycle()
            val timeOffsetMs by viewModel.timeOffsetMs.collectAsStateWithLifecycle()
            val fontSizeSp by viewModel.fontSizeSp.collectAsStateWithLifecycle()
            val subtitleStylePreset by viewModel.subtitleStylePreset.collectAsStateWithLifecycle()
            val subtitleAlignment by viewModel.subtitleAlignment.collectAsStateWithLifecycle()
            val fullscreenDisplayMode by viewModel.fullscreenDisplayMode.collectAsStateWithLifecycle()
            val cueSearchQuery by viewModel.cueSearchQuery.collectAsStateWithLifecycle()

            val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
            val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
            val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
            val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
            val isFullscreen by viewModel.isFullscreen.collectAsStateWithLifecycle()
            val isSentenceLoop by viewModel.isSentenceLoop.collectAsStateWithLifecycle()
            val isAutoPause by viewModel.isAutoPause.collectAsStateWithLifecycle()
            val starredCueIndices by viewModel.starredCueIndices.collectAsStateWithLifecycle()
            val geminiNotes by viewModel.projectGeminiNotes.collectAsStateWithLifecycle()

            // PDF & Reels Reading States
            val activePdfBook by viewModel.activePdfBook.collectAsStateWithLifecycle()
            val isReelsMode by viewModel.isReelsMode.collectAsStateWithLifecycle()
            val pdfCurrentPage by viewModel.pdfCurrentPage.collectAsStateWithLifecycle()
            val pdfReadingTheme by viewModel.pdfReadingTheme.collectAsStateWithLifecycle()
            val pdfStarredPages by viewModel.pdfStarredPages.collectAsStateWithLifecycle()
            val pdfPageSentences by viewModel.pdfPageSentences.collectAsStateWithLifecycle()
            val isPdfSpeaking by viewModel.speechManager.isSpeaking.collectAsStateWithLifecycle()
            val activeSentenceIndex by viewModel.speechManager.activeSentenceIndex.collectAsStateWithLifecycle()
            val pdfSpeechSpeed by viewModel.speechManager.speechSpeed.collectAsStateWithLifecycle()
            val openAiSummaries by viewModel.allOpenAiSummaries.collectAsStateWithLifecycle(initialValue = emptyList())
            val dictionaryTerms by viewModel.allDictionaryTerms.collectAsStateWithLifecycle(initialValue = emptyList())
            val comparativeTerms by viewModel.allComparativeTerms.collectAsStateWithLifecycle(initialValue = emptyList())
            val verbPhrasalTerms by viewModel.allVerbPhrasalTerms.collectAsStateWithLifecycle(initialValue = emptyList())
            val pageTranslations by viewModel.allPageTranslations.collectAsStateWithLifecycle(initialValue = emptyList())
            val bookQuestions by viewModel.allBookQuestions.collectAsStateWithLifecycle(initialValue = emptyList())
            val isGeneratingQuestions by viewModel.isGeneratingQuestions.collectAsStateWithLifecycle()
            val questionsGenerationError by viewModel.questionsGenerationError.collectAsStateWithLifecycle()
            val bookRecommendations by viewModel.allBookRecommendations.collectAsStateWithLifecycle(initialValue = emptyList())
            val isGeneratingBookRecommendations by viewModel.isGeneratingBookRecommendations.collectAsStateWithLifecycle()
            val bookRecommendationsError by viewModel.bookRecommendationsError.collectAsStateWithLifecycle()
            val isClassifyingBook by viewModel.isClassifyingBook.collectAsStateWithLifecycle()
            val allAuthors by viewModel.allAuthors.collectAsStateWithLifecycle(initialValue = emptyList())
            val allSubjects by viewModel.allSubjects.collectAsStateWithLifecycle(initialValue = emptyList())
            val allHighlights by viewModel.allHighlights.collectAsStateWithLifecycle(initialValue = emptyList())

            SrtTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        isFullscreen && activeProject != null -> {
                            FullScreenSubtitleView(
                                projectTitle = activeProject!!.title,
                                cues = cues,
                                secondaryCues = secondaryCues,
                                currentPositionMs = currentPositionMs,
                                timeOffsetMs = timeOffsetMs,
                                fontSizeSp = fontSizeSp,
                                subtitleStylePreset = subtitleStylePreset,
                                subtitleAlignment = subtitleAlignment,
                                displayMode = fullscreenDisplayMode,
                                isPlaying = isPlaying,
                                durationMs = durationMs,
                                playbackSpeed = playbackSpeed,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSeek = { viewModel.seekTo(it) },
                                onAdjustOffsetMs = { viewModel.adjustTimeOffset(it) },
                                onSetFontSize = { viewModel.setFontSize(it) },
                                onChangeSpeed = { viewModel.changePlaybackSpeed(it) },
                                onSetStylePreset = { viewModel.setSubtitleStylePreset(it) },
                                onSetAlignment = { viewModel.setSubtitleAlignment(it) },
                                onSetDisplayMode = { viewModel.setFullscreenDisplayMode(it) },
                                onLoadSecondarySrt = { viewModel.loadSecondarySrtContent(it) },
                                onExitFullscreen = { viewModel.toggleFullscreen() }
                            )
                        }

                        activeProject != null -> {
                            EditorScreen(
                                project = activeProject!!,
                                cues = cues,
                                secondaryCues = secondaryCues,
                                folders = folders,
                                timeOffsetMs = timeOffsetMs,
                                fontSizeSp = fontSizeSp,
                                subtitleStylePreset = subtitleStylePreset,
                                subtitleAlignment = subtitleAlignment,
                                cueSearchQuery = cueSearchQuery,
                                isPlaying = isPlaying,
                                currentPositionMs = currentPositionMs,
                                durationMs = durationMs,
                                playbackSpeed = playbackSpeed,
                                isSentenceLoop = isSentenceLoop,
                                isAutoPause = isAutoPause,
                                starredCueIndices = starredCueIndices,
                                geminiNotes = geminiNotes,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSeek = { viewModel.seekTo(it) },
                                onChangeSpeed = { viewModel.changePlaybackSpeed(it) },
                                onToggleSentenceLoop = { viewModel.toggleSentenceLoop() },
                                onToggleAutoPause = { viewModel.toggleAutoPause() },
                                onToggleStarCue = { viewModel.toggleStarCue(it) },
                                onSaveGeminiNote = { cueIdx, term, context, resp ->
                                    viewModel.saveGeminiNote(cueIdx, term, context, resp)
                                },
                                onDeleteGeminiNote = { viewModel.deleteGeminiNote(it) },
                                onDeleteGeminiNoteForCue = { viewModel.deleteGeminiNoteForCue(it) },
                                onSplitCue = { idx, splitTimeMs -> viewModel.splitCue(idx, splitTimeMs) },
                                onMergeCueWithNext = { viewModel.mergeWithNextCue(it) },
                                onDeleteCue = { viewModel.deleteCue(it) },
                                onAddCue = { startMs, endMs, text -> viewModel.addCustomCue(startMs, endMs, text) },
                                onAdjustOffsetMs = { viewModel.adjustTimeOffset(it) },
                                onSetOffsetMs = { viewModel.setTimeOffset(it) },
                                onSetFontSize = { viewModel.setFontSize(it) },
                                onSetStylePreset = { viewModel.setSubtitleStylePreset(it) },
                                onSetAlignment = { viewModel.setSubtitleAlignment(it) },
                                onSearchCueQueryChange = { viewModel.setCueSearchQuery(it) },
                                onToggleFullscreen = { viewModel.toggleFullscreen() },
                                onUpdateCue = { idx, text, startMs, endMs ->
                                    viewModel.updateCue(idx, text, startMs, endMs)
                                },
                                onMoveToFolder = { proj, folderId -> viewModel.updateProjectFolder(proj, folderId) },
                                onUpdateTitle = { viewModel.updateProjectTitle(activeProject!!, it) },
                                onUpdateProjectIcon = { iconUri -> viewModel.updateProjectIcon(activeProject!!, iconUri) },
                                onLoadSecondarySrt = { viewModel.loadSecondarySrtContent(it) },
                                onCloseProject = { viewModel.closeProject() }
                            )
                        }

                        (isReelsMode || activePdfBook != null) && pdfBooks.isNotEmpty() -> {
                            val initialIdx = remember(activePdfBook?.id) {
                                activePdfBook?.let { b -> pdfBooks.indexOfFirst { it.id == b.id } }?.coerceAtLeast(0) ?: 0
                            }
                            ReelsBookPagerScreen(
                                books = pdfBooks,
                                initialBookIndex = initialIdx,
                                readingTheme = pdfReadingTheme,
                                speechManager = viewModel.speechManager,
                                isSpeaking = isPdfSpeaking,
                                activeSentenceIndex = activeSentenceIndex,
                                speechSpeed = pdfSpeechSpeed,
                                starredPages = pdfStarredPages,
                                geminiNotes = geminiNotes,
                                openAiSummaries = openAiSummaries,
                                dictionaryTerms = dictionaryTerms,
                                comparativeTerms = comparativeTerms,
                                verbPhrasalTerms = verbPhrasalTerms,
                                onSaveVerbPhrasalTerms = { viewModel.saveVerbPhrasalTerms(it) },
                                onToggleFavoriteVerb = { viewModel.toggleFavoriteVerbPhrasalTerm(it) },
                                pageTranslations = pageTranslations,
                                onCloseReels = {
                                    viewModel.setReelsMode(false)
                                    viewModel.closePdfBook()
                                },
                                onPageChanged = { book, page ->
                                    if (activePdfBook?.id != book.id) {
                                        viewModel.openPdfBook(book.copy(currentPage = page), startInReels = true)
                                    } else {
                                        viewModel.changePdfPage(page)
                                    }
                                },
                                onToggleSpeech = { viewModel.togglePdfSpeech() },
                                onSpeedChange = { viewModel.setPdfSpeechSpeed(it) },
                                onThemeChange = { viewModel.setPdfReadingTheme(it) },
                                onToggleStarPage = { viewModel.toggleStarPdfPage(it) },
                                onSaveGeminiNote = { cueIdx, term, context, resp, targetBookId ->
                                    viewModel.saveGeminiNote(cueIdx, term, context, resp, targetBookId = targetBookId)
                                },
                                onDeleteGeminiNoteForCue = { cueIdx, targetBookId ->
                                    viewModel.deleteGeminiNoteForCue(cueIdx, targetBookId = targetBookId)
                                },
                                onSaveOpenAiSummary = { summary ->
                                    viewModel.saveOpenAiSummary(summary)
                                },
                                onDeleteOpenAiSummary = { summary ->
                                    viewModel.deleteOpenAiSummary(summary)
                                },
                                onSaveDictionaryTerms = { terms ->
                                    viewModel.saveDictionaryTerms(terms)
                                },
                                onReplaceDictionaryTermsForPage = { bookId, page, terms ->
                                    viewModel.replaceDictionaryTermsForPage(bookId, page, terms)
                                },
                                onUpdateDictionaryTerm = { term ->
                                    viewModel.saveDictionaryTerm(term)
                                },
                                onDeleteDictionaryTerm = { term ->
                                    viewModel.deleteDictionaryTerm(term)
                                },
                                onDeleteDictionaryTermsForPage = { bookId, page ->
                                    viewModel.deleteDictionaryTermsForPage(bookId, page)
                                },
                                onSaveComparativeTerms = { terms ->
                                    viewModel.saveComparativeTerms(terms)
                                },
                                onReplaceComparativeTermsForPage = { bookId, page, terms ->
                                    viewModel.replaceComparativeTermsForPage(bookId, page, terms)
                                },
                                onUpdateComparativeTerm = { term ->
                                    viewModel.saveComparativeTerm(term)
                                },
                                onDeleteComparativeTerm = { term ->
                                    viewModel.deleteComparativeTerm(term)
                                },
                                onDeleteComparativeTermsForPage = { bookId, page ->
                                    viewModel.deleteComparativeTermsForPage(bookId, page)
                                },
                                onSavePageTranslation = { translation ->
                                    viewModel.savePageTranslation(translation)
                                },
                                onDeletePageTranslation = { translation ->
                                    viewModel.deletePageTranslation(translation)
                                },
                                onDeletePageTranslationsForPage = { bookId, page ->
                                    viewModel.deletePageTranslationForPage(bookId, page)
                                },
                                bookQuestions = bookQuestions,
                                isGeneratingQuestions = isGeneratingQuestions,
                                questionsGenerationError = questionsGenerationError,
                                onGenerateQuestions = { book, startPage, endPage, inst, diff, count, model ->
                                    viewModel.generateQuestionsForBook(
                                        context = this@MainActivity,
                                        book = book,
                                        startPage = startPage,
                                        endPage = endPage,
                                        institution = inst,
                                        difficulty = diff,
                                        questionCount = count,
                                        model = model
                                    )
                                },
                                onAnswerQuestion = { qId, selIdx, isCorrect ->
                                    viewModel.recordQuestionAnswer(qId, selIdx, isCorrect)
                                },
                                onResetQuestionAnswer = { qId ->
                                    viewModel.resetQuestionAnswer(qId)
                                },
                                onResetAllAnswers = { bookId ->
                                    viewModel.resetAllAnswersForBook(bookId)
                                },
                                onResetAllQuestionsGlobal = {
                                    viewModel.resetAllQuestionsGlobal()
                                },
                                onDeleteQuestion = { qId ->
                                    viewModel.deleteQuestion(qId)
                                },
                                onDeleteQuestionsForPage = { bookId, page ->
                                    viewModel.deleteQuestionsForPage(bookId, page)
                                },
                                onDeleteAllQuestionsForBook = { bookId ->
                                    viewModel.deleteQuestionsForBook(bookId)
                                },
                                bookRecommendations = bookRecommendations,
                                isGeneratingBookRecommendations = isGeneratingBookRecommendations,
                                bookRecommendationsError = bookRecommendationsError,
                                onGenerateBookRecommendations = { book, page, text, force ->
                                    viewModel.generateBookRecommendationsForPage(
                                        context = this@MainActivity,
                                        book = book,
                                        pageNumber = page,
                                        pageText = text,
                                        forceRegenerate = force
                                    )
                                },
                                onDeleteBookRecommendationsForPage = { bookId, page ->
                                    viewModel.deleteBookRecommendationsForPage(bookId, page)
                                },
                                isClassifyingBook = isClassifyingBook,
                                onReclassifyCategory = { book ->
                                    viewModel.classifyBookWithOpenAi(book, forceReclassify = true)
                                },
                                onSelectCategory = { bookId, category, subcategory, colorHex ->
                                    viewModel.updateBookCategory(bookId, category, subcategory, colorHex)
                                },
                                onWordClick = { book, sIdx, wIdx, sentences ->
                                    viewModel.speakBookFromWord(book, book.currentPage, sIdx, wIdx, sentences)
                                },
                                authors = allAuthors,
                                onSaveAuthor = { author, bookIds ->
                                    viewModel.saveAuthor(author) { newId ->
                                        val authorId = if (author.id != 0) author.id else newId.toInt()
                                        viewModel.assignBooksToAuthor(authorId, bookIds)
                                    }
                                },
                                onDeleteAuthor = { author ->
                                    viewModel.deleteAuthor(author)
                                },
                                onAssignBooksToAuthor = { authorId, bookIds ->
                                    viewModel.assignBooksToAuthor(authorId, bookIds)
                                },
                                subjects = allSubjects,
                                onSaveSubject = { subject, authorIds, directBookIds ->
                                    viewModel.saveSubject(subject, authorIds, directBookIds)
                                },
                                onDeleteSubject = { subject ->
                                    viewModel.deleteSubject(subject)
                                },
                                onUpdateSubjectAssociations = { subjectId, authorIds, directBookIds ->
                                    viewModel.updateSubjectAssociations(subjectId, authorIds, directBookIds)
                                },
                                onAddDirectBooksToSubject = { subjectId, items ->
                                    viewModel.createMultipleBooksForSubject(subjectId, items)
                                },
                                highlights = allHighlights,
                                onSaveHighlight = { viewModel.saveHighlight(it) },
                                onDeleteHighlight = { viewModel.deleteHighlight(it) },
                                viewModel = viewModel
                            )
                        }

                        else -> {
                            HomeScreen(
                                projects = projects,
                                folders = folders,
                                pdfBooks = pdfBooks,
                                selectedFolderId = selectedFolderId,
                                searchQuery = searchQuery,
                                isDarkMode = isDarkMode,
                                onSelectFolder = { viewModel.selectFolder(it) },
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                onToggleDarkMode = { viewModel.toggleDarkMode() },
                                onOpenProject = { viewModel.openProject(it) },
                                onOpenPdfBook = { book, startInReels -> viewModel.openPdfBook(book, startInReels) },
                                onCreateProject = { title, fId, mediaType, mediaUri, srtText, secondarySrtText, customIconUri ->
                                    viewModel.createProject(title, fId, mediaType, mediaUri, srtText, secondarySrtText, customIconUri)
                                },
                                onCreatePdfBook = { title, author, fileUri, totalPages, sampleJson ->
                                    viewModel.createPdfBook(title, author, fileUri, totalPages, sampleJson)
                                },
                                onCreateMultiplePdfBooks = { items ->
                                    viewModel.createMultiplePdfBooks(items)
                                },
                                authors = allAuthors,
                                onRenameProject = { proj, newTitle -> viewModel.updateProjectTitle(proj, newTitle) },
                                onDeleteProject = { viewModel.deleteProject(it) },
                                onDeletePdfBook = { viewModel.deletePdfBook(it) },
                                onRenamePdfBook = { book, newTitle -> viewModel.renamePdfBook(book, newTitle) },
                                onUpdatePdfBookCover = { book, coverUri -> viewModel.updatePdfBookCover(book, coverUri) },
                                onUpdateProjectIcon = { proj, iconUri -> viewModel.updateProjectIcon(proj, iconUri) },
                                onCreateFolder = { name, color -> viewModel.createFolder(name, color) },
                                onDeleteFolder = { viewModel.deleteFolder(it) },
                                onSaveQuestions = { viewModel.saveQuestions(it) },
                                onSaveDictionaryTerms = { viewModel.saveDictionaryTerms(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
