package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BookHighlight
import com.example.data.model.Folder
import com.example.data.model.GeminiNote
import com.example.data.model.OpenAiPageSummary
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.PageSegmentExplanation
import com.example.data.model.PdfBook
import com.example.data.model.Project
import com.example.data.model.SegmentChatMessage
import com.example.data.model.SubtitleCue
import com.example.data.openai.OpenAiService
import com.example.data.parser.SrtParser
import com.example.data.pdf.PdfRendererHelper
import com.example.data.repository.ProjectRepository
import com.example.data.speech.PdfSpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ProjectRepository(
        db.projectDao(),
        db.folderDao(),
        db.geminiNoteDao(),
        db.pdfBookDao(),
        db.openAiPageSummaryDao(),
        db.pageDictionaryTermDao(),
        db.comparativeLanguageTermDao(),
        db.pageMultiTranslationDao(),
        db.bookQuestionDao(),
        db.bookPageRecommendationDao(),
        db.scientificPageReconstructionDao(),
        db.authorCollectionDao(),
        db.bookHighlightDao(),
        db.subjectDisciplineDao(),
        db.pageSegmentExplanationDao(),
        db.segmentChatMessageDao(),
        db.savedSentenceAnalysisDao(),
        db.verbPhrasalVerbDao(),
        db.publicOrganizationDao()
    )

    val allFolders = repository.allFolders
    val allProjects = repository.allProjects
    val allGeminiNotes = repository.allGeminiNotes
    val allPdfBooks = repository.allPdfBooks
    val allOpenAiSummaries = repository.allOpenAiSummaries
    val allDictionaryTerms = repository.allDictionaryTerms
    val allComparativeTerms = repository.allComparativeTerms
    val allPageTranslations = repository.allPageTranslations
    val allBookQuestions = repository.allBookQuestions
    val allBookRecommendations = repository.allBookRecommendations
    val allAuthors = repository.allAuthors
    val allHighlights = repository.allHighlights
    val allSubjects = repository.allSubjects
    val allOrganizations = repository.allOrganizations
    val allSegmentExplanations: Flow<List<PageSegmentExplanation>> = repository.allSegmentExplanations
    val allSegmentChatMessages: Flow<List<SegmentChatMessage>> = repository.allSegmentChatMessages
    val allSavedSentenceAnalyses: Flow<List<com.example.data.model.SavedSentenceAnalysis>> = repository.allSavedSentenceAnalyses
    val allVerbPhrasalTerms: Flow<List<com.example.data.model.VerbPhrasalVerbStudyTerm>> = repository.allVerbPhrasalTerms

    fun savePublicOrganization(org: com.example.data.model.PublicOrganization) = viewModelScope.launch {
        repository.saveOrganization(org)
    }

    fun deletePublicOrganization(org: com.example.data.model.PublicOrganization) = viewModelScope.launch {
        repository.deleteOrganization(org)
    }

    fun addPdfToOrganization(orgId: Int, bookId: Int) = viewModelScope.launch {
        repository.addPdfBookToOrganization(orgId, bookId)
    }

    fun removePdfFromOrganization(orgId: Int, bookId: Int) = viewModelScope.launch {
        repository.removePdfBookFromOrganization(orgId, bookId)
    }

    fun insertHighlight(highlight: BookHighlight) = viewModelScope.launch {
        repository.insertHighlight(highlight)
    }

    fun updateHighlight(highlight: BookHighlight) = viewModelScope.launch {
        repository.updateHighlight(highlight)
    }

    fun saveHighlight(highlight: BookHighlight) = viewModelScope.launch {
        if (highlight.id == 0) {
            repository.insertHighlight(highlight)
        } else {
            repository.updateHighlight(highlight)
        }
    }

    fun deleteHighlight(highlight: BookHighlight) = viewModelScope.launch {
        repository.deleteHighlight(highlight)
    }

    fun deleteHighlightById(id: Int) = viewModelScope.launch {
        repository.deleteHighlightById(id)
    }

    val speechManager = PdfSpeechManager(application)
    val studyAlarmManager = com.example.data.alarm.StudyAlarmManager(application)
    val studyAlarmSettings = studyAlarmManager.settings
    val activeAlarmEvent = studyAlarmManager.activeAlarmEvent

    fun updateStudyAlarmSettings(
        enabled: Boolean,
        intervalMinutes: Int,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
        customAudioUri: String? = null,
        customAudioName: String? = null
    ) {
        studyAlarmManager.updateSettings(
            enabled = enabled,
            intervalMinutes = intervalMinutes,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            customAudioUri = customAudioUri,
            customAudioName = customAudioName
        )
    }

    fun dismissActiveStudyAlarm() {
        studyAlarmManager.dismissActiveAlarm()
    }

    fun testTriggerStudyAlarmNow() {
        viewModelScope.launch {
            val books = repository.allPdfBooks.first()
            studyAlarmManager.testTriggerNow(books)
        }
    }

    // Active PDF State
    private val _activePdfBook = MutableStateFlow<PdfBook?>(null)
    val activePdfBook: StateFlow<PdfBook?> = _activePdfBook.asStateFlow()

    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _isReelsMode = MutableStateFlow(true)
    val isReelsMode: StateFlow<Boolean> = _isReelsMode.asStateFlow()

    private val _pdfCurrentPage = MutableStateFlow(1)
    val pdfCurrentPage: StateFlow<Int> = _pdfCurrentPage.asStateFlow()

    private val _pdfReadingTheme = MutableStateFlow(prefs.getString("global_pdf_reading_theme", "MONOCHROME_BW") ?: "MONOCHROME_BW")
    val pdfReadingTheme: StateFlow<String> = _pdfReadingTheme.asStateFlow()

    private val _isClassifyingBook = MutableStateFlow(false)
    val isClassifyingBook: StateFlow<Boolean> = _isClassifyingBook.asStateFlow()

    private val _pdfStarredPages = MutableStateFlow<Set<Int>>(emptySet())
    val pdfStarredPages: StateFlow<Set<Int>> = _pdfStarredPages.asStateFlow()

    private val _pdfPageSentences = MutableStateFlow<List<String>>(emptyList())
    val pdfPageSentences: StateFlow<List<String>> = _pdfPageSentences.asStateFlow()

    private val _pdfTableOfContents = MutableStateFlow<List<com.example.data.pdf.PdfTocItem>>(emptyList())
    val pdfTableOfContents: StateFlow<List<com.example.data.pdf.PdfTocItem>> = _pdfTableOfContents.asStateFlow()

    private val _isExtractingPdfText = MutableStateFlow(false)
    val isExtractingPdfText: StateFlow<Boolean> = _isExtractingPdfText.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Int?>(null)
    val selectedFolderId: StateFlow<Int?> = _selectedFolderId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _activeProject = MutableStateFlow<Project?>(null)
    val activeProject: StateFlow<Project?> = _activeProject.asStateFlow()

    private val _projectGeminiNotes = MutableStateFlow<List<GeminiNote>>(emptyList())
    val projectGeminiNotes: StateFlow<List<GeminiNote>> = _projectGeminiNotes.asStateFlow()

    private var geminiNotesJob: Job? = null

    private val _cues = MutableStateFlow<List<SubtitleCue>>(emptyList())
    val cues: StateFlow<List<SubtitleCue>> = _cues.asStateFlow()

    private val _secondaryCues = MutableStateFlow<List<SubtitleCue>>(emptyList())
    val secondaryCues: StateFlow<List<SubtitleCue>> = _secondaryCues.asStateFlow()

    private val _timeOffsetMs = MutableStateFlow(0L)
    val timeOffsetMs: StateFlow<Long> = _timeOffsetMs.asStateFlow()

    private val _fontSizeSp = MutableStateFlow(24f)
    val fontSizeSp: StateFlow<Float> = _fontSizeSp.asStateFlow()

    private val _subtitleStylePreset = MutableStateFlow(com.example.data.model.SubtitleStylePreset.CLASSIC_WHITE)
    val subtitleStylePreset: StateFlow<com.example.data.model.SubtitleStylePreset> = _subtitleStylePreset.asStateFlow()

    private val _subtitleAlignment = MutableStateFlow(com.example.data.model.SubtitleAlignment.CENTER)
    val subtitleAlignment: StateFlow<com.example.data.model.SubtitleAlignment> = _subtitleAlignment.asStateFlow()

    private val _fullscreenDisplayMode = MutableStateFlow(com.example.data.model.FullscreenDisplayMode.CONTINUOUS_3PARTS)
    val fullscreenDisplayMode: StateFlow<com.example.data.model.FullscreenDisplayMode> = _fullscreenDisplayMode.asStateFlow()

    private val _cueSearchQuery = MutableStateFlow("")
    val cueSearchQuery: StateFlow<String> = _cueSearchQuery.asStateFlow()

    // Study & Reading Enhancement States
    private val _isSentenceLoop = MutableStateFlow(false)
    val isSentenceLoop: StateFlow<Boolean> = _isSentenceLoop.asStateFlow()

    private val _isAutoPause = MutableStateFlow(false)
    val isAutoPause: StateFlow<Boolean> = _isAutoPause.asStateFlow()

    private val _starredCueIndices = MutableStateFlow<Set<Int>>(emptySet())
    val starredCueIndices: StateFlow<Set<Int>> = _starredCueIndices.asStateFlow()

    private val _onlyShowStarred = MutableStateFlow(false)
    val onlyShowStarred: StateFlow<Boolean> = _onlyShowStarred.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(20_000L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null

    init {
        com.example.data.pdf.PdfRendererHelper.clearTextCache()
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            repository.checkAndSeedInitialOrganizations()
            syncAllOfflineLawsToBooks()
        }
        viewModelScope.launch {
            repository.allGeminiNotes.collectLatest { notes ->
                _projectGeminiNotes.value = notes
            }
        }
        viewModelScope.launch {
            repository.allPdfBooks.collectLatest { books ->
                studyAlarmManager.setBooksSupplier { books }
            }
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun selectFolder(folderId: Int?) {
        _selectedFolderId.value = folderId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openProject(project: Project) {
        saveActiveProjectState()
        _activeProject.value = project
        _timeOffsetMs.value = project.timeOffsetMs
        _fontSizeSp.value = project.fontSizeSp
        val parsed = SrtParser.parseSrt(project.srtContent)
        _cues.value = parsed

        val parsedSecondary = if (!project.secondarySrtContent.isNullOrBlank()) {
            SrtParser.parseSrt(project.secondarySrtContent)
        } else {
            emptyList()
        }
        _secondaryCues.value = parsedSecondary

        releaseMediaPlayer()

        if (!project.mediaUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(project.mediaUri)
                val mp = MediaPlayer()
                mp.setDataSource(getApplication(), uri)
                mp.prepare()
                _durationMs.value = mp.duration.toLong().coerceAtLeast(1000L)
                mp.setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    stopPlaybackLoop()
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                e.printStackTrace()
                val maxTime = parsed.maxOfOrNull { it.endTimeMs } ?: 20_000L
                _durationMs.value = (maxTime + 2000L).coerceAtLeast(10_000L)
            }
        } else {
            val maxTime = parsed.maxOfOrNull { it.endTimeMs } ?: 20_000L
            _durationMs.value = (maxTime + 2000L).coerceAtLeast(10_000L)
        }

        _currentPositionMs.value = 0L
        _isPlaying.value = false
        stopPlaybackLoop()

        // Observe Gemini Notes for this active project
        geminiNotesJob?.cancel()
        geminiNotesJob = viewModelScope.launch {
            repository.getNotesForProject(project.id).collectLatest { notes ->
                _projectGeminiNotes.value = notes
                // Also ensure cues with notes are included in starred set for easy discovery
                val noteCueIndices = notes.map { it.cueIndex }.toSet()
                if (noteCueIndices.isNotEmpty()) {
                    _starredCueIndices.value = _starredCueIndices.value + noteCueIndices
                }
            }
        }
    }

    fun closeProject() {
        saveActiveProjectState()
        geminiNotesJob?.cancel()
        geminiNotesJob = null
        _projectGeminiNotes.value = emptyList()
        stopPlaybackLoop()
        releaseMediaPlayer()
        _activeProject.value = null
        _isFullscreen.value = false
    }

    fun saveGeminiNote(
        cueIndex: Int,
        termOrSentence: String,
        fullContext: String,
        geminiResponse: String,
        targetBookId: Int? = null
    ) {
        val projId = targetBookId ?: _activePdfBook.value?.id ?: _activeProject.value?.id ?: 1
        viewModelScope.launch {
            val existing = repository.getNoteByTermAndProject(projId, termOrSentence) ?: repository.getNoteForCue(projId, cueIndex)
            val note = GeminiNote(
                id = existing?.id ?: 0,
                projectId = projId,
                cueIndex = cueIndex,
                termOrSentence = termOrSentence,
                fullContext = fullContext,
                geminiResponse = geminiResponse,
                createdAt = System.currentTimeMillis()
            )
            repository.saveGeminiNote(note)
            // Star the cue when note is saved
            val current = _starredCueIndices.value.toMutableSet()
            current.add(cueIndex)
            _starredCueIndices.value = current
        }
    }

    fun deleteGeminiNote(note: GeminiNote) {
        viewModelScope.launch {
            repository.deleteGeminiNote(note)
        }
    }

    fun deleteGeminiNoteForCue(cueIndex: Int, targetBookId: Int? = null) {
        val projId = targetBookId ?: _activePdfBook.value?.id ?: _activeProject.value?.id ?: 1
        viewModelScope.launch {
            repository.deleteGeminiNoteForCue(projId, cueIndex)
        }
    }

    fun togglePlayPause() {
        val nextState = !_isPlaying.value
        _isPlaying.value = nextState

        if (mediaPlayer != null) {
            try {
                if (nextState) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val params = mediaPlayer?.playbackParams ?: PlaybackParams()
                            params.speed = _playbackSpeed.value
                            mediaPlayer?.playbackParams = params
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    mediaPlayer?.start()
                    startPlaybackLoop()
                } else {
                    mediaPlayer?.pause()
                    stopPlaybackLoop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            if (nextState) {
                startPlaybackLoop()
            } else {
                stopPlaybackLoop()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val bounded = positionMs.coerceIn(0L, _durationMs.value)
        _currentPositionMs.value = bounded
        if (mediaPlayer != null) {
            try {
                mediaPlayer?.seekTo(bounded.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun changePlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        if (mediaPlayer != null && _isPlaying.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = mediaPlayer?.playbackParams ?: PlaybackParams()
                params.speed = speed
                mediaPlayer?.playbackParams = params
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun adjustTimeOffset(deltaMs: Long) {
        val newOffset = _timeOffsetMs.value + deltaMs
        _timeOffsetMs.value = newOffset
        saveActiveProjectState()
    }

    fun setTimeOffset(offsetMs: Long) {
        _timeOffsetMs.value = offsetMs
        saveActiveProjectState()
    }

    fun setFontSize(sp: Float) {
        _fontSizeSp.value = sp
        saveActiveProjectState()
    }

    fun setSubtitleStylePreset(preset: com.example.data.model.SubtitleStylePreset) {
        _subtitleStylePreset.value = preset
    }

    fun setSubtitleAlignment(alignment: com.example.data.model.SubtitleAlignment) {
        _subtitleAlignment.value = alignment
    }

    fun setFullscreenDisplayMode(mode: com.example.data.model.FullscreenDisplayMode) {
        _fullscreenDisplayMode.value = mode
    }

    fun setCueSearchQuery(query: String) {
        _cueSearchQuery.value = query
    }

    fun toggleFullscreen() {
        _isFullscreen.value = !_isFullscreen.value
    }

    fun toggleSentenceLoop() {
        _isSentenceLoop.value = !_isSentenceLoop.value
    }

    fun toggleAutoPause() {
        _isAutoPause.value = !_isAutoPause.value
    }

    fun toggleStarCue(cueIndex: Int) {
        val current = _starredCueIndices.value.toMutableSet()
        if (current.contains(cueIndex)) {
            current.remove(cueIndex)
        } else {
            current.add(cueIndex)
        }
        _starredCueIndices.value = current
    }

    fun toggleOnlyShowStarred() {
        _onlyShowStarred.value = !_onlyShowStarred.value
    }

    fun splitCue(cueIndex: Int, splitAtMs: Long) {
        val currentList = _cues.value.toMutableList()
        val idx = currentList.indexOfFirst { it.index == cueIndex }
        if (idx != -1) {
            val oldCue = currentList[idx]
            val midTime = splitAtMs.coerceIn(oldCue.startTimeMs + 300L, oldCue.endTimeMs - 300L)
            val words = oldCue.text.split(Regex("\\s+")).filter { it.isNotBlank() }
            val half = (words.size / 2).coerceAtLeast(1)
            val text1 = words.take(half).joinToString(" ")
            val text2 = words.drop(half).joinToString(" ").ifBlank { "..." }

            val cue1 = oldCue.copy(
                text = text1,
                endTimeMs = midTime,
                words = SrtParser.generateWordTimings(text1, oldCue.startTimeMs, midTime)
            )
            val cue2 = SubtitleCue(
                index = oldCue.index + 1,
                startTimeMs = midTime + 50L,
                endTimeMs = oldCue.endTimeMs,
                text = text2,
                words = SrtParser.generateWordTimings(text2, midTime + 50L, oldCue.endTimeMs)
            )

            currentList[idx] = cue1
            currentList.add(idx + 1, cue2)

            // Re-index
            val reindexed = currentList.mapIndexed { i, c -> c.copy(index = i + 1) }
            _cues.value = reindexed
            saveActiveProjectState()
        }
    }

    fun mergeWithNextCue(cueIndex: Int) {
        val currentList = _cues.value.toMutableList()
        val idx = currentList.indexOfFirst { it.index == cueIndex }
        if (idx != -1 && idx < currentList.size - 1) {
            val cue1 = currentList[idx]
            val cue2 = currentList[idx + 1]
            val mergedText = "${cue1.text} ${cue2.text}".trim()
            val mergedWords = SrtParser.generateWordTimings(mergedText, cue1.startTimeMs, cue2.endTimeMs)
            val mergedCue = cue1.copy(
                text = mergedText,
                endTimeMs = cue2.endTimeMs,
                words = mergedWords
            )
            currentList[idx] = mergedCue
            currentList.removeAt(idx + 1)

            val reindexed = currentList.mapIndexed { i, c -> c.copy(index = i + 1) }
            _cues.value = reindexed
            saveActiveProjectState()
        }
    }

    fun deleteCue(cueIndex: Int) {
        val currentList = _cues.value.toMutableList()
        val idx = currentList.indexOfFirst { it.index == cueIndex }
        if (idx != -1 && currentList.size > 1) {
            currentList.removeAt(idx)
            val reindexed = currentList.mapIndexed { i, c -> c.copy(index = i + 1) }
            _cues.value = reindexed
            saveActiveProjectState()
        }
    }

    fun addCustomCue(startTimeMs: Long, endTimeMs: Long, text: String) {
        val currentList = _cues.value.toMutableList()
        val newCue = SubtitleCue(
            index = currentList.size + 1,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            text = text,
            words = SrtParser.generateWordTimings(text, startTimeMs, endTimeMs)
        )
        currentList.add(newCue)
        val sorted = currentList.sortedBy { it.startTimeMs }.mapIndexed { i, c -> c.copy(index = i + 1) }
        _cues.value = sorted
        saveActiveProjectState()
    }

    fun addCueAtCurrentTime() {
        val currentList = _cues.value.toMutableList()
        val curPos = (_currentPositionMs.value - _timeOffsetMs.value).coerceAtLeast(0L)
        val endPos = curPos + 3000L
        val newCue = SubtitleCue(
            index = currentList.size + 1,
            startTimeMs = curPos,
            endTimeMs = endPos,
            text = "Nova legenda",
            words = SrtParser.generateWordTimings("Nova legenda", curPos, endPos)
        )
        currentList.add(newCue)
        val sorted = currentList.sortedBy { it.startTimeMs }.mapIndexed { i, c -> c.copy(index = i + 1) }
        _cues.value = sorted
        saveActiveProjectState()
    }

    fun restoreBackup(importedProjects: List<Project>, importedFolders: List<Folder>) {
        viewModelScope.launch {
            importedFolders.forEach { f ->
                repository.insertFolder(f)
            }
            importedProjects.forEach { p ->
                repository.insertProject(p)
            }
        }
    }

    fun updateCue(cueIndex: Int, newText: String, newStartMs: Long, newEndMs: Long) {
        val currentList = _cues.value.toMutableList()
        val index = currentList.indexOfFirst { it.index == cueIndex }
        if (index != -1) {
            val oldCue = currentList[index]
            val updatedWords = SrtParser.generateWordTimings(newText, newStartMs, newEndMs)
            currentList[index] = oldCue.copy(
                text = newText,
                startTimeMs = newStartMs,
                endTimeMs = newEndMs,
                words = updatedWords
            )
            _cues.value = currentList
            val updatedSrt = SrtParser.exportSrt(currentList, 0L)
            _activeProject.value = _activeProject.value?.copy(srtContent = updatedSrt)
            saveActiveProjectState()
        }
    }

    fun updateProjectMedia(mediaUri: String, mediaType: String) {
        val proj = _activeProject.value ?: return
        val updated = proj.copy(mediaUri = mediaUri, mediaType = mediaType)
        _activeProject.value = updated
        openProject(updated)
    }

    fun updateProjectIcon(project: Project, iconUri: String?) {
        viewModelScope.launch {
            val updated = project.copy(customIconUri = iconUri, lastModified = System.currentTimeMillis())
            repository.updateProject(updated)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = updated
            }
        }
    }

    fun updateActiveProjectIcon(iconUri: String?) {
        val proj = _activeProject.value ?: return
        updateProjectIcon(proj, iconUri)
    }

    fun updateProjectTitle(project: Project, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            val updated = project.copy(title = newTitle.trim(), lastModified = System.currentTimeMillis())
            repository.updateProject(updated)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = updated
            }
        }
    }

    fun updateActiveProjectTitle(newTitle: String) {
        val proj = _activeProject.value ?: return
        if (newTitle.isBlank()) return
        updateProjectTitle(proj, newTitle)
    }

    fun loadSrtContent(srtText: String) {
        val proj = _activeProject.value ?: return
        val parsed = SrtParser.parseSrt(srtText)
        _cues.value = parsed
        val updated = proj.copy(srtContent = srtText)
        _activeProject.value = updated
        saveActiveProjectState()
    }

    fun loadSecondarySrtContent(srtText: String) {
        val proj = _activeProject.value ?: return
        val parsed = SrtParser.parseSrt(srtText)
        _secondaryCues.value = parsed
        val updated = proj.copy(secondarySrtContent = srtText)
        _activeProject.value = updated
        saveActiveProjectState()
    }

    fun createProject(
        title: String,
        folderId: Int?,
        mediaType: String,
        mediaUri: String?,
        srtText: String,
        secondarySrtText: String? = null,
        customIconUri: String? = null
    ) {
        viewModelScope.launch {
            val defaultSrt = if (srtText.isNotBlank()) srtText else """
1
00:00:01,000 --> 00:00:04,000
Digite ou importe sua legenda SRT aqui.

2
00:00:04,500 --> 00:00:08,000
As palavras serão destacadas em tempo real!
            """.trimIndent()

            val newProj = Project(
                title = if (title.isNotBlank()) title else "Novo Projeto",
                folderId = folderId ?: _selectedFolderId.value,
                mediaUri = mediaUri,
                mediaType = mediaType,
                srtContent = defaultSrt,
                secondarySrtContent = secondarySrtText,
                customIconUri = customIconUri,
                lastModified = System.currentTimeMillis()
            )
            val newId = repository.insertProject(newProj)
            val created = newProj.copy(id = newId.toInt())
            openProject(created)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            if (_activeProject.value?.id == project.id) {
                closeProject()
            }
            repository.deleteProject(project)
        }
    }

    fun updateProjectFolder(project: Project, newFolderId: Int?) {
        viewModelScope.launch {
            val updated = project.copy(folderId = newFolderId)
            repository.updateProject(updated)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = updated
            }
        }
    }

    fun createFolder(name: String, colorHex: String) {
        viewModelScope.launch {
            val folder = Folder(name = name, colorHex = colorHex)
            repository.insertFolder(folder)
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            if (_selectedFolderId.value == folder.id) {
                _selectedFolderId.value = null
            }
        }
    }

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            var lastActiveCueIndex: Int? = null
            while (isActive && _isPlaying.value) {
                val currentPos = if (mediaPlayer != null) {
                    try {
                        val pos = mediaPlayer?.currentPosition?.toLong() ?: _currentPositionMs.value
                        _currentPositionMs.value = pos
                        pos
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _currentPositionMs.value
                    }
                } else {
                    val increment = (50L * _playbackSpeed.value).toLong()
                    val nextPos = _currentPositionMs.value + increment
                    if (nextPos >= _durationMs.value) {
                        _currentPositionMs.value = 0L
                        _isPlaying.value = false
                        break
                    } else {
                        _currentPositionMs.value = nextPos
                        nextPos
                    }
                }

                // Check Sentence Loop & Auto-Pause
                val offset = _timeOffsetMs.value
                val activeCue = _cues.value.firstOrNull { it.isActiveAt(currentPos, offset) }

                if (activeCue != null) {
                    val cueStart = activeCue.getAdjustedStart(offset)
                    val cueEnd = activeCue.getAdjustedEnd(offset)

                    if (_isSentenceLoop.value && currentPos >= cueEnd - 100L) {
                        // Loop back to start of this sentence
                        seekTo(cueStart)
                    } else if (_isAutoPause.value && currentPos >= cueEnd - 100L && lastActiveCueIndex == activeCue.index) {
                        // Pause at the end of the sentence (dictation mode)
                        togglePlayPause()
                        lastActiveCueIndex = null
                    } else {
                        lastActiveCueIndex = activeCue.index
                    }
                }

                delay(50L)
            }
        }
    }

    private fun stopPlaybackLoop() {
        playbackJob?.cancel()
        playbackJob = null
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
    }

    private fun saveActiveProjectState() {
        val proj = _activeProject.value ?: return
        val currentSrt = SrtParser.exportSrt(_cues.value, 0L)
        val secSrt = if (_secondaryCues.value.isNotEmpty()) SrtParser.exportSrt(_secondaryCues.value, 0L) else proj.secondarySrtContent
        val updated = proj.copy(
            srtContent = currentSrt,
            secondarySrtContent = secSrt,
            timeOffsetMs = _timeOffsetMs.value,
            fontSizeSp = _fontSizeSp.value,
            lastModified = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.updateProject(updated)
        }
    }

    // ==========================================
    // PDF & E-BOOK READING + REELS FUNCTIONS
    // ==========================================

    private var loadPdfJob: kotlinx.coroutines.Job? = null

    fun openPdfBook(book: PdfBook, startInReels: Boolean = true) {
        // Cancel any pending load job from previous book
        loadPdfJob?.cancel()
        // Stop any previous speech immediately, clear sentences and reset state
        speechManager.stop()
        speechManager.resetActiveIndex()

        val globalTheme = prefs.getString("global_pdf_reading_theme", _pdfReadingTheme.value) ?: _pdfReadingTheme.value
        _activePdfBook.value = book.copy(readingTheme = globalTheme)
        _isReelsMode.value = true
        _pdfCurrentPage.value = book.currentPage
        _pdfReadingTheme.value = globalTheme
        _pdfPageSentences.value = emptyList()
        _pdfTableOfContents.value = emptyList()
        speechManager.setSpeed(book.speechSpeed)

        // Parse starred pages
        val starred = try {
            val jsonArr = JSONArray(book.starredPagesJson)
            val set = mutableSetOf<Int>()
            for (i in 0 until jsonArr.length()) {
                set.add(jsonArr.getInt(i))
            }
            set
        } catch (e: Exception) {
            emptySet()
        }
        _pdfStarredPages.value = starred

        loadPdfPageContent(book, book.currentPage, autoStartIfWasSpeaking = false)

        // Observe Gemini Notes specifically for this active book
        geminiNotesJob?.cancel()
        geminiNotesJob = viewModelScope.launch {
            repository.getNotesForProject(book.id).collectLatest { notes ->
                _projectGeminiNotes.value = notes
            }
        }

        // If the book does not have a category yet, trigger classification automatically via OpenAI
        if (book.category.isNullOrBlank()) {
            classifyBookWithOpenAi(book)
        }

        // Generate Table of Contents asynchronously
        viewModelScope.launch {
            val toc = com.example.data.pdf.PdfRendererHelper.generateTableOfContents(getApplication(), book)
            _pdfTableOfContents.value = toc
        }
    }

    fun closePdfBook() {
        speechManager.stop()
        val book = _activePdfBook.value
        if (book != null) {
            viewModelScope.launch {
                repository.updatePdfProgress(book.id, _pdfCurrentPage.value)
            }
        }
        geminiNotesJob?.cancel()
        geminiNotesJob = null
        _projectGeminiNotes.value = emptyList()
        _activePdfBook.value = null
        _pdfTableOfContents.value = emptyList()
        _isReelsMode.value = false
        _pdfPageSentences.value = emptyList()
        _pdfTableOfContents.value = emptyList()
    }

    fun setReelsMode(enabled: Boolean) {
        _isReelsMode.value = enabled
    }

    fun classifyBookWithOpenAi(
        book: PdfBook,
        customBitmap: Bitmap? = null,
        forceReclassify: Boolean = false
    ) {
        viewModelScope.launch {
            // Se o livro já foi classificado e salvo no banco local (Room SQLite), e não foi requisitada
            // reclassificação explícita, mantém a classificação existente funcionando 100% offline.
            if (!forceReclassify && !book.category.isNullOrBlank()) {
                Log.d("MainViewModel", "Livro '${book.title}' já está salvo com a categoria '${book.category}'. Operando offline.")
                return@launch
            }

            _isClassifyingBook.value = true
            try {
                val context = getApplication<Application>()
                val coverBitmap = customBitmap ?: if (!book.fileUri.isNullOrBlank()) {
                    try {
                        withContext(Dispatchers.IO) {
                            PdfRendererHelper.renderPdfPageBitmap(
                                context = context,
                                fileUri = book.fileUri,
                                pageIndexZeroBased = 0,
                                destWidth = 512,
                                readingTheme = "MONOCHROME_BW"
                            )
                        }
                    } catch (e: Exception) {
                        null
                    }
                } else null

                val sampleText = try {
                    if (!book.sampleContentJson.isNullOrBlank()) {
                        val arr = JSONArray(book.sampleContentJson)
                        arr.optString(0, "")
                    } else null
                } catch (_: Exception) { null }

                val result = OpenAiService.classifyBookCategory(
                    context = context,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    coverBitmap = coverBitmap,
                    sampleText = sampleText
                ).getOrNull()

                if (result != null) {
                    repository.updatePdfCategory(
                        bookId = book.id,
                        category = result.category,
                        subcategory = result.subcategory,
                        color = result.colorHex
                    )
                    val current = _activePdfBook.value
                    if (current != null && current.id == book.id) {
                        _activePdfBook.value = current.copy(
                            category = result.category,
                            subcategory = result.subcategory,
                            categoryColor = result.colorHex
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error classifying book: ${e.message}", e)
            } finally {
                _isClassifyingBook.value = false
            }
        }
    }

    fun updateBookCategory(bookId: Int, category: String?, subcategory: String?, color: String?) {
        viewModelScope.launch {
            repository.updatePdfCategory(bookId, category, subcategory, color)
            val current = _activePdfBook.value
            if (current != null && current.id == bookId) {
                _activePdfBook.value = current.copy(
                    category = category,
                    subcategory = subcategory,
                    categoryColor = color
                )
            }
        }
    }

    fun createPdfBook(title: String, author: String, fileUri: String?, totalPages: Int, sampleJson: String? = null) {
        viewModelScope.launch {
            val globalTheme = prefs.getString("global_pdf_reading_theme", _pdfReadingTheme.value) ?: "MONOCHROME_BW"
            val newBook = PdfBook(
                title = title.ifBlank { "Livro sem título" },
                author = author.ifBlank { "Documento PDF" },
                fileUri = fileUri,
                totalPages = totalPages.coerceAtLeast(1),
                currentPage = 1,
                readingTheme = globalTheme,
                sampleContentJson = sampleJson,
                dateAdded = System.currentTimeMillis(),
                lastReadTimestamp = System.currentTimeMillis()
            )
            val id = repository.insertPdfBook(newBook)
            val created = newBook.copy(id = id.toInt())
            classifyBookWithOpenAi(created)
            openPdfBook(created, startInReels = true)
        }
    }

    /**
     * Sincroniza qualquer lei, decreto ou documento baixado ou acessado na Legislação
     * diretamente como um livro no banco de dados, categorizado e formatado para leitura no modo Reels.
     */
    fun syncLawBookToDatabase(
        title: String,
        author: String,
        sampleJson: String,
        totalPages: Int,
        category: String,
        subcategory: String,
        color: String,
        sourceUrl: String
    ) {
        viewModelScope.launch {
            try {
                val existing = repository.allPdfBooks.first().firstOrNull {
                    (it.fileUri != null && it.fileUri == sourceUrl) || it.title.equals(title, ignoreCase = true)
                }
                if (existing != null) {
                    val updated = existing.copy(
                        title = title,
                        fileUri = sourceUrl,
                        totalPages = totalPages.coerceAtLeast(1),
                        sampleContentJson = sampleJson,
                        category = category,
                        subcategory = subcategory,
                        categoryColor = color,
                        lastReadTimestamp = System.currentTimeMillis()
                    )
                    repository.updatePdfBook(updated)
                } else {
                    val newBook = PdfBook(
                        title = title.ifBlank { "Legislação Planalto" },
                        author = author.ifBlank { "Presidência da República / Planalto" },
                        fileUri = sourceUrl,
                        totalPages = totalPages.coerceAtLeast(1),
                        currentPage = 1,
                        readingTheme = "DARK_OLED",
                        sampleContentJson = sampleJson,
                        category = category,
                        subcategory = subcategory,
                        categoryColor = color,
                        dateAdded = System.currentTimeMillis(),
                        lastReadTimestamp = System.currentTimeMillis()
                    )
                    repository.insertPdfBook(newBook)
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Erro ao sincronizar livro de legislação: ${e.message}")
            }
        }
    }

    /**
     * Varre todo o cache offline de legislação salva e garante que todos os documentos baixados
     * estejam convertidos como livros na grade e no carrossel de capas dos Reels.
     */
    fun syncAllOfflineLawsToBooks() {
        viewModelScope.launch {
            try {
                val cachedLaws = com.example.data.legislation.LawOfflineCacheManager.getAllCachedLawsList(getApplication())
                val existingBooks = repository.allPdfBooks.first()

                for (cachedDoc in cachedLaws) {
                    val alreadyExists = existingBooks.any {
                        it.fileUri == cachedDoc.url || it.title.equals(cachedDoc.title, ignoreCase = true)
                    }
                    if (!alreadyExists) {
                        val fullDoc = com.example.data.legislation.LawOfflineCacheManager.getCachedLaw(getApplication(), cachedDoc.url)
                        if (fullDoc != null && fullDoc.articles.isNotEmpty()) {
                            val cleanText = fullDoc.articles.joinToString("\n\n") { "${it.header} ${it.content}".trim() }
                            val pages = com.example.data.legislation.PlanaltoLegislationHelper.paginateLawContent(cleanText)
                            val jsonArray = JSONArray()
                            pages.forEach { jsonArray.put(it) }

                            val classification = com.example.data.legislation.PlanaltoLegislationHelper.classifyLawDocument(fullDoc.title, fullDoc.url)

                            val newBook = PdfBook(
                                title = fullDoc.title.ifBlank { "Legislação Salva" },
                                author = "Presidência da República / Planalto",
                                fileUri = fullDoc.url,
                                totalPages = pages.size.coerceAtLeast(1),
                                currentPage = 1,
                                readingTheme = "DARK_OLED",
                                sampleContentJson = jsonArray.toString(),
                                category = classification.category,
                                subcategory = classification.subcategory,
                                categoryColor = classification.categoryColor,
                                dateAdded = fullDoc.timestamp,
                                lastReadTimestamp = fullDoc.timestamp
                            )
                            repository.insertPdfBook(newBook)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Erro ao converter leis offline em livros: ${e.message}")
            }
        }
    }

    data class MultiPdfBookItem(
        val title: String,
        val author: String = "Documento",
        val fileUri: String?,
        val totalPages: Int,
        val sampleJson: String? = null,
        val authorCollectionId: Int? = null
    )

    fun createMultiplePdfBooks(
        items: List<MultiPdfBookItem>,
        openFirstBook: Boolean = true,
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
        onFinished: (() -> Unit)? = null
    ) {
        if (items.isEmpty()) {
            onFinished?.invoke()
            return
        }
        viewModelScope.launch {
            val globalTheme = prefs.getString("global_pdf_reading_theme", _pdfReadingTheme.value) ?: "MONOCHROME_BW"
            var firstCreated: PdfBook? = null
            for ((idx, item) in items.withIndex()) {
                val newBook = PdfBook(
                    title = item.title.ifBlank { "Livro sem título" },
                    author = item.author.ifBlank { "Autor Desconhecido" },
                    fileUri = item.fileUri,
                    totalPages = item.totalPages.coerceAtLeast(1),
                    currentPage = 1,
                    readingTheme = globalTheme,
                    sampleContentJson = item.sampleJson,
                    dateAdded = System.currentTimeMillis() + idx,
                    lastReadTimestamp = System.currentTimeMillis(),
                    authorCollectionId = item.authorCollectionId
                )
                val id = repository.insertPdfBook(newBook)
                val created = newBook.copy(id = id.toInt())
                classifyBookWithOpenAi(created)
                if (firstCreated == null) {
                    firstCreated = created
                }
                onProgress?.invoke(idx + 1, items.size)
            }
            onFinished?.invoke()
            if (openFirstBook && firstCreated != null) {
                openPdfBook(firstCreated, startInReels = true)
            }
        }
    }

    fun deletePdfBook(book: PdfBook) {
        viewModelScope.launch {
            if (_activePdfBook.value?.id == book.id) {
                closePdfBook()
            }
            com.example.data.pdf.PdfRendererHelper.clearTocCache(book.id)
            repository.deletePdfBook(book)
        }
    }

    fun renamePdfBook(book: PdfBook, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val updated = book.copy(title = trimmed)
            repository.updatePdfBook(updated)
            if (_activePdfBook.value?.id == book.id) {
                _activePdfBook.value = updated
            }
        }
    }

    fun updatePdfBookCover(book: PdfBook, coverUri: String) {
        if (coverUri.isBlank()) return
        viewModelScope.launch {
            val updated = book.copy(coverPath = coverUri)
            repository.updatePdfBook(updated)
            if (_activePdfBook.value?.id == book.id) {
                _activePdfBook.value = updated
            }
        }
    }

    fun changePdfPage(page: Int, autoReadIfWasSpeaking: Boolean = true) {
        val book = _activePdfBook.value ?: return
        val clampedPage = page.coerceIn(1, book.totalPages)
        val wasSpeaking = speechManager.isSpeaking.value || autoReadIfWasSpeaking
        _pdfCurrentPage.value = clampedPage
        _activePdfBook.value = book.copy(currentPage = clampedPage)
        
        // Stop previous utterance immediately and reset active sentence to top (index 0)
        speechManager.pause()
        speechManager.resetActiveIndex()
        loadPdfPageContent(_activePdfBook.value!!, clampedPage, autoStartIfWasSpeaking = wasSpeaking && autoReadIfWasSpeaking)
        viewModelScope.launch {
            repository.updatePdfProgress(book.id, clampedPage)
        }
    }

    fun nextPage() {
        val book = _activePdfBook.value ?: return
        if (_pdfCurrentPage.value < book.totalPages) {
            changePdfPage(_pdfCurrentPage.value + 1)
        }
    }

    fun previousPage() {
        if (_pdfCurrentPage.value > 1) {
            changePdfPage(_pdfCurrentPage.value - 1)
        }
    }

    fun setPdfReadingTheme(theme: String) {
        _pdfReadingTheme.value = theme
        prefs.edit().putString("global_pdf_reading_theme", theme).apply()
        viewModelScope.launch {
            repository.updateAllPdfReadingThemes(theme)
            _activePdfBook.value?.let { current ->
                _activePdfBook.value = current.copy(readingTheme = theme)
            }
        }
    }

    fun setPdfSpeechSpeed(speed: Float) {
        speechManager.setSpeed(speed)
        val book = _activePdfBook.value ?: return
        viewModelScope.launch {
            repository.updatePdfSpeechSpeed(book.id, speed)
        }
    }

    fun toggleStarPdfPage(page: Int) {
        val current = _pdfStarredPages.value.toMutableSet()
        if (current.contains(page)) {
            current.remove(page)
        } else {
            current.add(page)
        }
        _pdfStarredPages.value = current

        val book = _activePdfBook.value ?: return
        val json = JSONArray(current.toList()).toString()
        viewModelScope.launch {
            repository.updatePdfStarredPages(book.id, json)
        }
    }

    fun togglePdfSpeech() {
        if (speechManager.isSpeaking.value) {
            speechManager.pause()
        } else {
            val book = _activePdfBook.value
            val sentences = _pdfPageSentences.value
            val currentPageNum = _pdfCurrentPage.value
            if (sentences.isNotEmpty()) {
                val curIdx = speechManager.activeSentenceIndex.value
                val startIdx = if (curIdx in sentences.indices) curIdx else 0
                speechManager.startReading(
                    sentences = sentences,
                    startIndex = startIdx,
                    onPageFinished = {
                        advanceToNextPageAndContinueReading(currentPageNum)
                    }
                )
            } else if (book != null) {
                loadPdfPageContent(book, currentPageNum, autoStartIfWasSpeaking = true)
            }
        }
    }

    fun speakFromSentence(index: Int) {
        speakFromWord(index, 0)
    }

    fun speakFromWord(sentenceIndex: Int, wordIndex: Int) {
        val book = _activePdfBook.value
        val sentences = _pdfPageSentences.value
        val currentPageNum = _pdfCurrentPage.value
        if (sentences.isNotEmpty()) {
            val safeSentenceIdx = sentenceIndex.coerceIn(0, sentences.size - 1)
            speechManager.startReading(
                sentences = sentences,
                startIndex = safeSentenceIdx,
                startWordIndex = wordIndex,
                onPageFinished = {
                    advanceToNextPageAndContinueReading(currentPageNum)
                }
            )
        } else if (book != null) {
            loadPdfPageContentAndSpeakFrom(book, currentPageNum, sentenceIndex, wordIndex)
        }
    }

    fun speakBookFromWord(
        book: PdfBook,
        pageNumber: Int,
        sentenceIndex: Int,
        wordIndex: Int,
        providedSentences: List<String>? = null
    ) {
        val isDifferentBookOrPage = _activePdfBook.value?.id != book.id || _pdfCurrentPage.value != pageNumber
        if (isDifferentBookOrPage) {
            loadPdfJob?.cancel()
            speechManager.stop()
            val globalTheme = prefs.getString("global_pdf_reading_theme", _pdfReadingTheme.value) ?: _pdfReadingTheme.value
            _activePdfBook.value = book.copy(currentPage = pageNumber, readingTheme = globalTheme)
            _pdfCurrentPage.value = pageNumber
            _pdfReadingTheme.value = globalTheme
            speechManager.setSpeed(book.speechSpeed)
            viewModelScope.launch {
                repository.updatePdfProgress(book.id, pageNumber)
            }
        }

        if (!providedSentences.isNullOrEmpty()) {
            _pdfPageSentences.value = providedSentences
            val safeSentenceIdx = sentenceIndex.coerceIn(0, providedSentences.size - 1)
            speechManager.startReading(
                sentences = providedSentences,
                startIndex = safeSentenceIdx,
                startWordIndex = wordIndex,
                onPageFinished = {
                    advanceToNextPageAndContinueReading(pageNumber)
                }
            )
        } else {
            loadPdfPageContentAndSpeakFrom(book, pageNumber, sentenceIndex, wordIndex)
        }
    }

    private fun loadPdfPageContentAndSpeakFrom(
        book: PdfBook,
        pageNumber: Int,
        sentenceIndex: Int,
        wordIndex: Int
    ) {
        loadPdfJob?.cancel()
        loadPdfJob = viewModelScope.launch {
            _isExtractingPdfText.value = true
            val sentences = com.example.data.pdf.PdfRendererHelper.extractTextFromPage(getApplication(), book, pageNumber)
            if (_activePdfBook.value?.id != book.id || _pdfCurrentPage.value != pageNumber) {
                _isExtractingPdfText.value = false
                return@launch
            }
            _pdfPageSentences.value = sentences
            _isExtractingPdfText.value = false
            if (sentences.isNotEmpty()) {
                val safeSentenceIdx = sentenceIndex.coerceIn(0, sentences.size - 1)
                speechManager.startReading(
                    sentences = sentences,
                    startIndex = safeSentenceIdx,
                    startWordIndex = wordIndex,
                    onPageFinished = {
                        advanceToNextPageAndContinueReading(pageNumber)
                    }
                )
            }
        }
    }

    private fun advanceToNextPageAndContinueReading(completedPageNumber: Int) {
        val book = _activePdfBook.value ?: return
        val nextPage = completedPageNumber + 1
        if (nextPage <= book.totalPages) {
            changePdfPage(nextPage, autoReadIfWasSpeaking = true)
        } else {
            speechManager.stop()
        }
    }

    private fun loadPdfPageContent(book: PdfBook, pageNumber: Int, autoStartIfWasSpeaking: Boolean = false) {
        loadPdfJob?.cancel()
        loadPdfJob = viewModelScope.launch {
            _isExtractingPdfText.value = true
            val sentences = com.example.data.pdf.PdfRendererHelper.extractTextFromPage(getApplication(), book, pageNumber)
            
            // Check if active book or page changed while extracting
            if (_activePdfBook.value?.id != book.id || _pdfCurrentPage.value != pageNumber) {
                _isExtractingPdfText.value = false
                return@launch
            }

            _pdfPageSentences.value = sentences
            _isExtractingPdfText.value = false

            // Pre-arm speechManager with sentences so any immediate word click plays without delay
            speechManager.prepareSentences(
                sentences = sentences,
                onPageFinished = {
                    advanceToNextPageAndContinueReading(pageNumber)
                }
            )

            if (autoStartIfWasSpeaking && sentences.isNotEmpty()) {
                // Ensure starting from the absolute first sentence/word at the top of the page
                speechManager.startReading(
                    sentences = sentences,
                    startIndex = 0,
                    onPageFinished = {
                        advanceToNextPageAndContinueReading(pageNumber)
                    }
                )
            }
        }
    }

    fun getSummaryForPage(bookId: Int, pageNumber: Int): Flow<OpenAiPageSummary?> {
        return repository.getSummaryForPage(bookId, pageNumber)
    }

    suspend fun getSummaryForPageSync(bookId: Int, pageNumber: Int): OpenAiPageSummary? {
        return repository.getSummaryForPageSync(bookId, pageNumber)
    }

    fun getAllSummariesForBook(bookId: Int): Flow<List<OpenAiPageSummary>> {
        return repository.getAllSummariesForBook(bookId)
    }

    fun saveOpenAiSummary(summary: OpenAiPageSummary) {
        viewModelScope.launch {
            repository.saveOpenAiSummary(summary)
        }
    }

    fun deleteOpenAiSummary(summary: OpenAiPageSummary) {
        viewModelScope.launch {
            // Also delete audio file if it exists
            summary.audioFilePath?.let { path ->
                try {
                    val file = java.io.File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
            repository.deleteOpenAiSummary(summary)
        }
    }

    fun deleteOpenAiSummaryForPage(bookId: Int, pageNumber: Int) {
        viewModelScope.launch {
            val existing = repository.getSummaryForPageSync(bookId, pageNumber)
            existing?.audioFilePath?.let { path ->
                try {
                    val file = java.io.File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
            repository.deleteOpenAiSummaryForPage(bookId, pageNumber)
        }
    }

    // --- Universal Dictionary Term Operations ---

    fun getDictionaryTermsForPage(bookId: Int, pageNumber: Int): Flow<List<PageDictionaryTerm>> {
        return repository.getDictionaryTermsForPage(bookId, pageNumber)
    }

    fun getDictionaryTermsForBook(bookId: Int): Flow<List<PageDictionaryTerm>> {
        return repository.getDictionaryTermsForBook(bookId)
    }

    fun searchDictionaryTerms(query: String): Flow<List<PageDictionaryTerm>> {
        return repository.searchDictionaryTerms(query)
    }

    fun replaceDictionaryTermsForPage(bookId: Int, pageNumber: Int, terms: List<PageDictionaryTerm>) {
        viewModelScope.launch {
            repository.replaceTermsForPage(bookId, pageNumber, terms)
        }
    }

    fun saveDictionaryTerms(terms: List<PageDictionaryTerm>) {
        viewModelScope.launch {
            repository.saveDictionaryTerms(terms)
        }
    }

    fun saveDictionaryTerm(term: PageDictionaryTerm) {
        viewModelScope.launch {
            repository.saveDictionaryTerm(term)
        }
    }

    fun deleteDictionaryTerm(term: PageDictionaryTerm) {
        viewModelScope.launch {
            repository.deleteDictionaryTerm(term)
        }
    }

    fun deleteDictionaryTermsForPage(bookId: Int, pageNumber: Int) {
        viewModelScope.launch {
            repository.deleteDictionaryTermsForPage(bookId, pageNumber)
        }
    }

    // --- Comparative Language Term Operations ---

    fun getComparativeTermsForPage(bookId: Int, pageNumber: Int): Flow<List<com.example.data.model.ComparativeLanguageTerm>> {
        return repository.getComparativeTermsForPage(bookId, pageNumber)
    }

    fun getComparativeTermsForBook(bookId: Int): Flow<List<com.example.data.model.ComparativeLanguageTerm>> {
        return repository.getComparativeTermsForBook(bookId)
    }

    fun replaceComparativeTermsForPage(bookId: Int, pageNumber: Int, terms: List<com.example.data.model.ComparativeLanguageTerm>) {
        viewModelScope.launch {
            repository.replaceComparativeTermsForPage(bookId, pageNumber, terms)
        }
    }

    fun saveComparativeTerms(terms: List<com.example.data.model.ComparativeLanguageTerm>) {
        viewModelScope.launch {
            repository.saveComparativeTerms(terms)
        }
    }

    fun saveComparativeTerm(term: com.example.data.model.ComparativeLanguageTerm) {
        viewModelScope.launch {
            repository.saveComparativeTerm(term)
        }
    }

    // --- Caderno de Estudos de Sintaxe e Gramática Poliglota ---

    fun getSavedSentenceAnalysesForBook(bookId: Int): Flow<List<com.example.data.model.SavedSentenceAnalysis>> {
        return repository.getSavedSentenceAnalysesForBook(bookId)
    }

    fun getSavedSentenceAnalysesByLanguage(langCode: String): Flow<List<com.example.data.model.SavedSentenceAnalysis>> {
        return repository.getSavedSentenceAnalysesByLanguage(langCode)
    }

    fun getFavoriteSentenceAnalyses(): Flow<List<com.example.data.model.SavedSentenceAnalysis>> {
        return repository.getFavoriteSentenceAnalyses()
    }

    fun saveSentenceAnalysis(analysis: com.example.data.model.SavedSentenceAnalysis, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.saveSentenceAnalysis(analysis)
            onComplete?.invoke(id)
        }
    }

    fun updateSentenceAnalysis(analysis: com.example.data.model.SavedSentenceAnalysis) {
        viewModelScope.launch {
            repository.updateSentenceAnalysis(analysis)
        }
    }

    fun deleteSentenceAnalysis(id: Long) {
        viewModelScope.launch {
            repository.deleteSentenceAnalysis(id)
        }
    }

    fun toggleFavoriteSentenceAnalysis(analysis: com.example.data.model.SavedSentenceAnalysis) {
        viewModelScope.launch {
            repository.toggleFavoriteSentenceAnalysis(analysis)
        }
    }

    // --- Caderno de Estudo de Verbos & Phrasal Verbs (16 Tempos Verbais & Question Words) ---
    fun saveVerbPhrasalTerms(terms: List<com.example.data.model.VerbPhrasalVerbStudyTerm>) {
        viewModelScope.launch {
            repository.saveVerbPhrasalTerms(terms)
        }
    }

    fun insertOrUpdateVerbPhrasalTerm(term: com.example.data.model.VerbPhrasalVerbStudyTerm, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.insertOrUpdateVerbPhrasalTerm(term)
            onComplete?.invoke(id)
        }
    }

    fun deleteVerbPhrasalTerm(term: com.example.data.model.VerbPhrasalVerbStudyTerm) {
        viewModelScope.launch {
            repository.deleteVerbPhrasalTerm(term)
        }
    }

    fun deleteVerbPhrasalTermsForPage(bookId: Int, pageNumber: Int) {
        viewModelScope.launch {
            repository.deleteVerbPhrasalTermsForPage(bookId, pageNumber)
        }
    }

    fun toggleFavoriteVerbPhrasalTerm(term: com.example.data.model.VerbPhrasalVerbStudyTerm) {
        viewModelScope.launch {
            repository.toggleFavoriteVerbPhrasalTerm(term)
        }
    }

    fun deleteComparativeTerm(term: com.example.data.model.ComparativeLanguageTerm) {
        viewModelScope.launch {
            repository.deleteComparativeTerm(term)
        }
    }

    fun deleteComparativeTermsForPage(bookId: Int, pageNumber: Int) {
        viewModelScope.launch {
            repository.deleteComparativeTermsForPage(bookId, pageNumber)
        }
    }

    // --- Page Multi-Translation (Trilateral Offline) Operations ---

    fun getTranslationForPage(bookId: Int, pageNumber: Int): kotlinx.coroutines.flow.Flow<com.example.data.model.PageMultiTranslation?> {
        return repository.getTranslationForPage(bookId, pageNumber)
    }

    suspend fun getTranslationForPageSync(bookId: Int, pageNumber: Int): com.example.data.model.PageMultiTranslation? {
        return repository.getTranslationForPageSync(bookId, pageNumber)
    }

    fun getTranslationsForBook(bookId: Int): kotlinx.coroutines.flow.Flow<List<com.example.data.model.PageMultiTranslation>> {
        return repository.getTranslationsForBook(bookId)
    }

    fun savePageTranslation(translation: com.example.data.model.PageMultiTranslation) {
        viewModelScope.launch {
            repository.savePageTranslation(translation)
        }
    }

    fun deletePageTranslation(translation: com.example.data.model.PageMultiTranslation) {
        viewModelScope.launch {
            repository.deletePageTranslation(translation)
        }
    }

    fun deletePageTranslationForPage(bookId: Int, pageNumber: Int) {
        viewModelScope.launch {
            repository.deletePageTranslationForPage(bookId, pageNumber)
        }
    }

    // --- Page Segment Explanations (Trilateral In-Place AI Explanations) ---

    private val _isGeneratingSegmentExplanation = MutableStateFlow(false)
    val isGeneratingSegmentExplanation: StateFlow<Boolean> = _isGeneratingSegmentExplanation.asStateFlow()

    private val _segmentExplanationError = MutableStateFlow<String?>(null)
    val segmentExplanationError: StateFlow<String?> = _segmentExplanationError.asStateFlow()

    fun getSegmentExplanationsForPage(bookId: Int, pageNumber: Int): Flow<List<PageSegmentExplanation>> {
        return repository.getSegmentExplanationsForPage(bookId, pageNumber)
    }

    fun saveSegmentExplanation(explanation: PageSegmentExplanation) {
        viewModelScope.launch {
            repository.saveSegmentExplanation(explanation)
        }
    }

    fun deleteSegmentExplanation(explanation: PageSegmentExplanation) {
        viewModelScope.launch {
            repository.deleteSegmentExplanationById(explanation.id)
        }
    }

    fun deleteSegmentExplanationForSegment(bookId: Int, pageNumber: Int, segmentIndex: Int) {
        viewModelScope.launch {
            repository.deleteSegmentExplanation(bookId, pageNumber, segmentIndex)
        }
    }

    fun deleteSegmentExplanationsForPage(bookId: Int, pageNumber: Int) {
        viewModelScope.launch {
            repository.deleteSegmentExplanationsForPage(bookId, pageNumber)
        }
    }

    /**
     * Gera ou busca a explicação daquele trecho via OpenAI.
     * Salva no banco de dados local daquela página e trecho.
     */
    fun requestSegmentExplanation(
        book: PdfBook,
        pageNumber: Int,
        segmentIndex: Int,
        segmentText: String,
        forceRegenerate: Boolean = false,
        onSuccess: (PageSegmentExplanation) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        requestSegmentExplanation(
            context = getApplication(),
            book = book,
            pageNumber = pageNumber,
            segmentIndex = segmentIndex,
            segmentText = segmentText,
            forceRegenerate = forceRegenerate,
            onSuccess = onSuccess,
            onError = onError
        )
    }

    fun requestSegmentExplanation(
        context: Context,
        book: PdfBook,
        pageNumber: Int,
        segmentIndex: Int,
        segmentText: String,
        forceRegenerate: Boolean = false,
        onSuccess: (PageSegmentExplanation) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!forceRegenerate) {
                val existing = repository.getSegmentExplanation(book.id, pageNumber, segmentIndex)
                if (existing != null && (existing.explanationPortuguese.isNotBlank() || existing.explanationSpanish.isNotBlank() || existing.explanationEnglish.isNotBlank() || existing.explanationFrench.isNotBlank())) {
                    onSuccess(existing)
                    return@launch
                }
            }

            val apiKey = com.example.data.openai.OpenAiService.getSavedApiKey(context)
            if (apiKey.isBlank()) {
                val err = "Chave da API OpenAI não configurada. Configure em Resumo OpenAI ou Configurações."
                _segmentExplanationError.value = err
                onError(err)
                return@launch
            }

            _isGeneratingSegmentExplanation.value = true
            _segmentExplanationError.value = null

            val result = com.example.data.openai.OpenAiService.generateSegmentExplanationTrilateral(
                apiKey = apiKey,
                bookTitle = book.title,
                pageNumber = pageNumber,
                segmentText = segmentText
            )

            _isGeneratingSegmentExplanation.value = false

            result.onSuccess { generated ->
                val full = generated.copy(
                    bookId = book.id,
                    pageNumber = pageNumber,
                    segmentIndex = segmentIndex,
                    segmentText = segmentText
                )
                repository.saveSegmentExplanation(full)
                onSuccess(full)
            }.onFailure { err ->
                val msg = err.message ?: "Erro desconhecido ao gerar explicação"
                _segmentExplanationError.value = msg
                onError(msg)
            }
        }
    }

    // --- Segment OpenAI Chat Operations ---

    private val _segmentChatLoadingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val segmentChatLoadingMap: StateFlow<Map<String, Boolean>> = _segmentChatLoadingMap.asStateFlow()

    private val _segmentChatErrorMap = MutableStateFlow<Map<String, String?>>(emptyMap())
    val segmentChatErrorMap: StateFlow<Map<String, String?>> = _segmentChatErrorMap.asStateFlow()

    fun isSegmentChatLoading(bookId: Int, pageNumber: Int, segmentIndex: Int): Boolean {
        val key = "$bookId-$pageNumber-$segmentIndex"
        return _segmentChatLoadingMap.value[key] == true
    }

    fun getSegmentChatError(bookId: Int, pageNumber: Int, segmentIndex: Int): String? {
        val key = "$bookId-$pageNumber-$segmentIndex"
        return _segmentChatErrorMap.value[key]
    }

    fun clearSegmentChatError(bookId: Int, pageNumber: Int, segmentIndex: Int) {
        val key = "$bookId-$pageNumber-$segmentIndex"
        _segmentChatErrorMap.update { it + (key to null) }
    }

    fun sendSegmentChatMessage(
        book: PdfBook,
        pageNumber: Int,
        segmentIndex: Int,
        segmentText: String,
        userQuestion: String,
        isAudioInput: Boolean = false
    ) {
        val q = userQuestion.trim()
        if (q.isBlank()) return

        val key = "${book.id}-$pageNumber-$segmentIndex"
        viewModelScope.launch {
            // 1. Grava no banco local do celular a pergunta imediatamente
            val userMsg = SegmentChatMessage(
                bookId = book.id,
                pageNumber = pageNumber,
                segmentIndex = segmentIndex,
                sender = "USER",
                messageText = q,
                isAudioInput = isAudioInput
            )
            repository.saveSegmentChatMessage(userMsg)

            _segmentChatLoadingMap.update { it + (key to true) }
            _segmentChatErrorMap.update { it + (key to null) }

            // 2. Coleta histórico recente daquela etapa
            val history = repository.getSegmentChatMessages(book.id, pageNumber, segmentIndex)
                .first()
                .map { it.sender to it.messageText }

            // 3. Comunica com o modelo OpenAI para resposta aprofundada e explicativa
            val apiKey = com.example.data.openai.OpenAiService.getSavedApiKey(getApplication())
            val res = com.example.data.openai.OpenAiService.sendSegmentChatMessage(
                apiKey = apiKey,
                model = "gpt-4o-mini",
                bookTitle = book.title,
                bookAuthor = book.author,
                pageNumber = pageNumber,
                segmentText = segmentText,
                history = history,
                userQuestion = q
            )

            res.onSuccess { reply ->
                val assistantMsg = SegmentChatMessage(
                    bookId = book.id,
                    pageNumber = pageNumber,
                    segmentIndex = segmentIndex,
                    sender = "ASSISTANT",
                    messageText = reply,
                    isAudioInput = false
                )
                repository.saveSegmentChatMessage(assistantMsg)
                _segmentChatLoadingMap.update { it + (key to false) }
            }.onFailure { err ->
                _segmentChatLoadingMap.update { it + (key to false) }
                val errorMsg = err.message ?: "Erro ao obter resposta da OpenAI."
                _segmentChatErrorMap.update { it + (key to errorMsg) }
            }
        }
    }

    fun clearSegmentChatMessages(bookId: Int, pageNumber: Int, segmentIndex: Int) {
        viewModelScope.launch {
            repository.clearSegmentChat(bookId, pageNumber, segmentIndex)
            val key = "$bookId-$pageNumber-$segmentIndex"
            _segmentChatErrorMap.update { it + (key to null) }
        }
    }

    fun deleteSegmentChatMessage(message: SegmentChatMessage) {
        viewModelScope.launch {
            repository.deleteSegmentChatMessage(message)
        }
    }

    // --- Book Questions (FGV, CEBRASPE, FCC, IADES) Operations ---

    private val _isGeneratingQuestions = MutableStateFlow(false)
    val isGeneratingQuestions: StateFlow<Boolean> = _isGeneratingQuestions.asStateFlow()

    private val _questionsGenerationError = MutableStateFlow<String?>(null)
    val questionsGenerationError: StateFlow<String?> = _questionsGenerationError.asStateFlow()

    fun clearQuestionsError() {
        _questionsGenerationError.value = null
    }

    fun saveQuestions(questions: List<com.example.data.model.BookQuestion>) {
        viewModelScope.launch {
            repository.saveQuestions(questions)
        }
    }

    fun saveQuestion(question: com.example.data.model.BookQuestion) {
        viewModelScope.launch {
            repository.saveQuestion(question)
        }
    }

    fun recordQuestionAnswer(questionId: Long, selectedIndex: Int, isCorrect: Boolean) {
        viewModelScope.launch {
            repository.recordQuestionAnswer(questionId, selectedIndex, isCorrect)
        }
    }

    fun resetQuestionAnswer(questionId: Long) {
        viewModelScope.launch {
            repository.resetQuestionAnswer(questionId)
        }
    }

    fun resetAllAnswersForBook(bookId: Int) {
        viewModelScope.launch {
            repository.resetAllAnswersForBook(bookId)
        }
    }

    fun resetAllQuestionsGlobal() {
        viewModelScope.launch {
            repository.resetAllQuestionsGlobal()
        }
    }

    fun deleteQuestion(questionId: Long) {
        viewModelScope.launch {
            repository.deleteQuestion(questionId)
        }
    }

    fun deleteQuestionsForPage(bookId: Int, page: Int) {
        viewModelScope.launch {
            repository.deleteQuestionsForPage(bookId, page)
        }
    }

    fun deleteQuestionsForBook(bookId: Int) {
        viewModelScope.launch {
            repository.deleteQuestionsForBook(bookId)
        }
    }

    fun generateQuestionsForBook(
        context: Context,
        book: PdfBook,
        startPage: Int,
        endPage: Int,
        institution: String,
        difficulty: String,
        questionCount: Int,
        model: String = "gpt-4o-mini",
        onSuccess: (List<com.example.data.model.BookQuestion>) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val apiKey = com.example.data.openai.OpenAiService.getSavedApiKey(context)
        if (apiKey.isBlank()) {
            val err = "Chave da API OpenAI não configurada. Configure em Resumo/Tradutor."
            _questionsGenerationError.value = err
            onError(err)
            return
        }

        viewModelScope.launch {
            _isGeneratingQuestions.value = true
            _questionsGenerationError.value = null

            try {
                // Extract text from the chosen page or range of pages
                val pagesText = (startPage..endPage).mapNotNull { page ->
                    val lines = com.example.data.pdf.PdfRendererHelper.extractTextFromPage(context, book, page)
                    val text = lines.joinToString("\n\n").ifBlank {
                        com.example.data.pdf.PdfRendererHelper.parseSamplePages(book.sampleContentJson).getOrNull(page - 1)
                    }
                    if (!text.isNullOrBlank()) {
                        "--- PÁGINA $page ---\n$text"
                    } else null
                }.joinToString("\n\n\n")

                if (pagesText.isBlank()) {
                    val err = "Não foi possível extrair texto legível das páginas selecionadas ($startPage a $endPage)."
                    _questionsGenerationError.value = err
                    _isGeneratingQuestions.value = false
                    onError(err)
                    return@launch
                }

                val result = com.example.data.openai.OpenAiService.generateBookQuestions(
                    apiKey = apiKey,
                    model = model,
                    bookId = book.id,
                    bookTitle = book.title,
                    startPage = startPage,
                    endPage = endPage,
                    contentSnippet = pagesText,
                    institution = institution,
                    difficulty = difficulty,
                    questionCount = questionCount
                )

                _isGeneratingQuestions.value = false

                result.onSuccess { questions ->
                    val insertedIds = repository.saveQuestions(questions)
                    val questionsWithIds = questions.mapIndexed { idx, q ->
                        val newId = insertedIds.getOrNull(idx) ?: q.id
                        if (newId > 0) q.copy(id = newId) else q
                    }
                    onSuccess(questionsWithIds)
                }.onFailure { ex ->
                    val err = ex.localizedMessage ?: "Erro ao gerar questões via OpenAI."
                    _questionsGenerationError.value = err
                    onError(err)
                }
            } catch (e: Exception) {
                _isGeneratingQuestions.value = false
                val err = e.localizedMessage ?: "Exceção inesperada: ${e.message}"
                _questionsGenerationError.value = err
                onError(err)
            }
        }
    }

    // -------------------------------------------------------------------------
    // BOOKS: Recomendações de Livros Reais via OpenAI e Análise Temática/Vírgulas
    // -------------------------------------------------------------------------
    private val _isGeneratingBookRecommendations = MutableStateFlow(false)
    val isGeneratingBookRecommendations: StateFlow<Boolean> = _isGeneratingBookRecommendations.asStateFlow()

    private val _bookRecommendationsError = MutableStateFlow<String?>(null)
    val bookRecommendationsError: StateFlow<String?> = _bookRecommendationsError.asStateFlow()

    fun generateBookRecommendationsForPage(
        context: Context,
        book: PdfBook,
        pageNumber: Int,
        pageText: String,
        forceRegenerate: Boolean = false,
        model: String = "gpt-4o-mini",
        onSuccess: (List<com.example.data.model.BookPageRecommendation>) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (_isGeneratingBookRecommendations.value) return@launch
            _isGeneratingBookRecommendations.value = true
            _bookRecommendationsError.value = null

            try {
                // Se já houver recomendações locais salvas no telefone e não for solicitação explícita de refazer
                if (!forceRegenerate) {
                    val existing = repository.getBookRecommendationsForPageSync(book.id, pageNumber)
                    if (existing.isNotEmpty()) {
                        _isGeneratingBookRecommendations.value = false
                        onSuccess(existing)
                        return@launch
                    }
                }

                // Obter o texto mais confiável da página (usando o texto recebido ou extraindo via helper)
                var resolvedText = pageText.trim()
                if (resolvedText.isBlank()) {
                    val (_, extracted) = com.example.data.pdf.PdfRendererHelper.extractParagraphsAndSentencesFromPage(
                        context, book, pageNumber
                    )
                    resolvedText = extracted.joinToString(" ").trim()
                }

                if (resolvedText.isBlank()) {
                    val err = "Não foi possível extrair o texto da página $pageNumber para análise de livros."
                    _bookRecommendationsError.value = err
                    _isGeneratingBookRecommendations.value = false
                    onError(err)
                    return@launch
                }

                val apiKey = com.example.data.openai.OpenAiService.getSavedApiKey(context)
                val result = com.example.data.openai.OpenAiService.generateBookRecommendations(
                    context = context,
                    apiKey = apiKey,
                    model = model,
                    bookId = book.id,
                    bookTitle = book.title,
                    pageNumber = pageNumber,
                    pageText = resolvedText
                )

                _isGeneratingBookRecommendations.value = false

                result.onSuccess { recommendations ->
                    repository.replaceBookRecommendationsForPage(book.id, pageNumber, recommendations)
                    onSuccess(recommendations)
                }.onFailure { ex ->
                    val err = ex.localizedMessage ?: "Erro ao gerar recomendações de livros."
                    _bookRecommendationsError.value = err
                    onError(err)
                }
            } catch (e: Exception) {
                _isGeneratingBookRecommendations.value = false
                val err = e.localizedMessage ?: "Exceção inesperada: ${e.message}"
                _bookRecommendationsError.value = err
                onError(err)
            }
        }
    }

    fun deleteBookRecommendationsForPage(bookId: Int, pageNumber: Int) {
        viewModelScope.launch {
            repository.deleteBookRecommendationsForPage(bookId, pageNumber)
        }
    }

    // -------------------------------------------------------------
    // RECONSTRUÇÃO CIENTÍFICA DE PÁGINA (Matemática, Química, Citologia, Física, Lógica)
    // -------------------------------------------------------------
    fun getScientificReconstructionForPage(bookId: Int, pageNumber: Int): Flow<com.example.data.model.ScientificPageReconstruction?> {
        return repository.getScientificReconstructionForPage(bookId, pageNumber)
    }

    fun saveScientificReconstruction(reconstruction: com.example.data.model.ScientificPageReconstruction) {
        viewModelScope.launch {
            repository.saveScientificReconstruction(reconstruction)
        }
    }

    fun deleteScientificReconstruction(bookId: Int, pageNumber: Int) {
        viewModelScope.launch {
            repository.deleteScientificReconstruction(bookId, pageNumber)
        }
    }

    fun applyScientificTextToReader(bookId: Int, pageNumber: Int, textToApply: String) {
        viewModelScope.launch {
            PdfRendererHelper.setCustomPageText(bookId, pageNumber, textToApply)
            val currentBook = _activePdfBook.value
            if (currentBook != null && currentBook.id == bookId && _pdfCurrentPage.value == pageNumber) {
                // Força recarregamento do conteúdo da página ativa com o texto reestruturado
                loadPdfPageContent(currentBook, pageNumber, autoStartIfWasSpeaking = false)
            }
        }
    }

    // -------------------------------------------------------------
    // AUTHORS & COLLECTIONS
    // -------------------------------------------------------------
    fun saveAuthor(author: com.example.data.model.AuthorCollection, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.saveAuthor(author)
            onComplete?.invoke(id)
        }
    }

    fun createAuthorWithBooks(
        author: com.example.data.model.AuthorCollection,
        existingBookIds: List<Int>,
        directBooks: List<MultiPdfBookItem> = emptyList(),
        onComplete: ((Int) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val authorId = repository.saveAuthor(author).toInt()
            if (existingBookIds.isNotEmpty()) {
                repository.assignBooksToAuthor(authorId, existingBookIds)
            }
            if (directBooks.isNotEmpty()) {
                val directWithAuthor = directBooks.map {
                    it.copy(author = author.name, authorCollectionId = authorId)
                }
                createMultiplePdfBooks(directWithAuthor, openFirstBook = false)
            }
            onComplete?.invoke(authorId)
        }
    }

    fun deleteAuthor(author: com.example.data.model.AuthorCollection) {
        viewModelScope.launch {
            repository.deleteAuthor(author)
        }
    }

    fun assignBookToAuthor(bookId: Int, authorId: Int?) {
        viewModelScope.launch {
            repository.setBookAuthorCollection(bookId, authorId)
        }
    }

    fun assignBooksToAuthor(authorId: Int, bookIds: List<Int>) {
        viewModelScope.launch {
            repository.assignBooksToAuthor(authorId, bookIds)
        }
    }

    fun removeBookFromAuthor(bookId: Int) {
        viewModelScope.launch {
            repository.removeBookFromAuthor(bookId)
        }
    }

    // -------------------------------------------------------------
    // SUBJECTS / DISCIPLINES
    // -------------------------------------------------------------
    fun saveSubject(
        subject: com.example.data.model.SubjectDiscipline,
        authorIds: List<Int> = emptyList(),
        directBookIds: List<Int> = emptyList(),
        onComplete: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val toSave = subject.copy(
                authorIdsJson = com.example.data.model.SubjectDiscipline.listToJson(authorIds),
                directBookIdsJson = com.example.data.model.SubjectDiscipline.listToJson(directBookIds)
            )
            val id = repository.saveSubject(toSave)
            onComplete?.invoke(id)
        }
    }

    fun deleteSubject(subject: com.example.data.model.SubjectDiscipline) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    fun updateSubjectAssociations(subjectId: Int, authorIds: List<Int>, directBookIds: List<Int>) {
        viewModelScope.launch {
            repository.updateSubjectAssociations(subjectId, authorIds, directBookIds)
        }
    }

    fun addDirectBookToSubject(subjectId: Int, bookId: Int) {
        viewModelScope.launch {
            repository.addDirectBookToSubject(subjectId, bookId)
        }
    }

    fun createMultipleBooksForSubject(
        subjectId: Int,
        items: List<MultiPdfBookItem>,
        onFinished: (() -> Unit)? = null
    ) {
        if (items.isEmpty()) {
            onFinished?.invoke()
            return
        }
        viewModelScope.launch {
            val globalTheme = prefs.getString("global_pdf_reading_theme", _pdfReadingTheme.value) ?: "MONOCHROME_BW"
            val newBookIds = mutableListOf<Int>()
            for ((idx, item) in items.withIndex()) {
                val newBook = PdfBook(
                    title = item.title.ifBlank { "Livro sem título" },
                    author = item.author.ifBlank { "Autor" },
                    fileUri = item.fileUri,
                    totalPages = item.totalPages.coerceAtLeast(1),
                    currentPage = 1,
                    readingTheme = globalTheme,
                    sampleContentJson = item.sampleJson,
                    dateAdded = System.currentTimeMillis() + idx,
                    lastReadTimestamp = System.currentTimeMillis(),
                    authorCollectionId = item.authorCollectionId
                )
                val id = repository.insertPdfBook(newBook)
                newBookIds.add(id.toInt())
                repository.addDirectBookToSubject(subjectId, id.toInt())
            }
            onFinished?.invoke()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.release()
        stopPlaybackLoop()
        releaseMediaPlayer()
    }
}
