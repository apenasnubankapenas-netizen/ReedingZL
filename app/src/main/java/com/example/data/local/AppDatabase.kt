package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AuthorCollection
import com.example.data.model.BookHighlight
import com.example.data.model.BookPageRecommendation
import com.example.data.model.BookQuestion
import com.example.data.model.ComparativeLanguageTerm
import com.example.data.model.Folder
import com.example.data.model.GeminiNote
import com.example.data.model.OpenAiPageSummary
import com.example.data.model.PageDictionaryTerm
import com.example.data.model.PageMultiTranslation
import com.example.data.model.PdfBook
import com.example.data.model.PageSegmentExplanation
import com.example.data.model.Project
import com.example.data.model.ScientificPageReconstruction
import com.example.data.model.SegmentChatMessage
import com.example.data.model.SubjectDiscipline
import com.example.data.model.SavedSentenceAnalysis
import com.example.data.model.VerbPhrasalVerbStudyTerm
import com.example.data.model.PublicOrganization

@Database(
    entities = [
        Project::class,
        Folder::class,
        GeminiNote::class,
        PdfBook::class,
        OpenAiPageSummary::class,
        PageDictionaryTerm::class,
        ComparativeLanguageTerm::class,
        PageMultiTranslation::class,
        BookQuestion::class,
        BookPageRecommendation::class,
        ScientificPageReconstruction::class,
        AuthorCollection::class,
        BookHighlight::class,
        SubjectDiscipline::class,
        PageSegmentExplanation::class,
        SegmentChatMessage::class,
        SavedSentenceAnalysis::class,
        VerbPhrasalVerbStudyTerm::class,
        PublicOrganization::class
    ],
    version = 23,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun folderDao(): FolderDao
    abstract fun geminiNoteDao(): GeminiNoteDao
    abstract fun pdfBookDao(): PdfBookDao
    abstract fun openAiPageSummaryDao(): OpenAiPageSummaryDao
    abstract fun pageDictionaryTermDao(): PageDictionaryTermDao
    abstract fun comparativeLanguageTermDao(): ComparativeLanguageTermDao
    abstract fun pageMultiTranslationDao(): PageMultiTranslationDao
    abstract fun bookQuestionDao(): BookQuestionDao
    abstract fun bookPageRecommendationDao(): BookPageRecommendationDao
    abstract fun scientificPageReconstructionDao(): ScientificPageReconstructionDao
    abstract fun authorCollectionDao(): AuthorCollectionDao
    abstract fun bookHighlightDao(): BookHighlightDao
    abstract fun subjectDisciplineDao(): SubjectDisciplineDao
    abstract fun pageSegmentExplanationDao(): PageSegmentExplanationDao
    abstract fun segmentChatMessageDao(): SegmentChatMessageDao
    abstract fun savedSentenceAnalysisDao(): SavedSentenceAnalysisDao
    abstract fun verbPhrasalVerbDao(): VerbPhrasalVerbDao
    abstract fun publicOrganizationDao(): PublicOrganizationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE pdf_books ADD COLUMN category TEXT")
                database.execSQL("ALTER TABLE pdf_books ADD COLUMN subcategory TEXT")
                database.execSQL("ALTER TABLE pdf_books ADD COLUMN categoryColor TEXT")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS page_multi_translations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bookId INTEGER NOT NULL,
                        pageNumber INTEGER NOT NULL,
                        originalText TEXT NOT NULL,
                        englishText TEXT NOT NULL,
                        frenchText TEXT NOT NULL,
                        spanishText TEXT NOT NULL,
                        comparativeParagraphsJson TEXT NOT NULL DEFAULT '',
                        translatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_page_multi_translations_bookId_pageNumber ON page_multi_translations(bookId, pageNumber)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_questions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bookId INTEGER NOT NULL,
                        startPage INTEGER NOT NULL,
                        endPage INTEGER NOT NULL,
                        institution TEXT NOT NULL,
                        difficulty TEXT NOT NULL,
                        questionType TEXT NOT NULL,
                        statement TEXT NOT NULL,
                        contextSnippet TEXT NOT NULL DEFAULT '',
                        optionsJson TEXT NOT NULL,
                        correctOptionIndex INTEGER NOT NULL,
                        explanation TEXT NOT NULL,
                        supportingQuote TEXT NOT NULL,
                        targetParagraph TEXT NOT NULL DEFAULT '',
                        userAnswerIndex INTEGER,
                        isAnswered INTEGER NOT NULL DEFAULT 0,
                        isCorrect INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_book_questions_bookId ON book_questions(bookId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_book_questions_bookId_startPage_endPage ON book_questions(bookId, startPage, endPage)")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_page_recommendations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bookId INTEGER NOT NULL,
                        pageNumber INTEGER NOT NULL,
                        themeOrTopic TEXT NOT NULL,
                        recommendedBookTitle TEXT NOT NULL,
                        author TEXT NOT NULL,
                        relevanceDescription TEXT NOT NULL,
                        analysisType TEXT NOT NULL DEFAULT 'PAGE_THEME',
                        yearOrEdition TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_book_page_recommendations_bookId_pageNumber ON book_page_recommendations(bookId, pageNumber)")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE page_multi_translations ADD COLUMN portugueseText TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE comparative_language_terms ADD COLUMN portugueseTerm TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE comparative_language_terms ADD COLUMN portugueseMeaning TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE comparative_language_terms ADD COLUMN portugueseIpa TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE comparative_language_terms ADD COLUMN audioPathPortuguese TEXT NOT NULL DEFAULT ''")

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS scientific_page_reconstructions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bookId INTEGER NOT NULL,
                        pageNumber INTEGER NOT NULL,
                        subjectArea TEXT NOT NULL DEFAULT 'Ciências Exatas & Biológicas',
                        pageTitle TEXT NOT NULL DEFAULT '',
                        structuredExplanation TEXT NOT NULL DEFAULT '',
                        formulasJson TEXT NOT NULL DEFAULT '',
                        diagramsJson TEXT NOT NULL DEFAULT '',
                        symbolsJson TEXT NOT NULL DEFAULT '',
                        ttsOptimizedText TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_scientific_reconstructions_bookId_pageNumber ON scientific_page_reconstructions(bookId, pageNumber)")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE scientific_page_reconstructions ADD COLUMN flashcardsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE pdf_books ADD COLUMN authorCollectionId INTEGER")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS author_collections (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        bio TEXT NOT NULL DEFAULT '',
                        photoUri TEXT,
                        bannerColorHex TEXT NOT NULL DEFAULT '#6366F1',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_highlights (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bookId INTEGER NOT NULL,
                        bookTitle TEXT NOT NULL,
                        bookAuthor TEXT NOT NULL,
                        pageNumber INTEGER NOT NULL,
                        quoteText TEXT NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        colorTag TEXT NOT NULL DEFAULT '#F59E0B',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_book_highlights_bookId ON book_highlights(bookId)")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS subject_disciplines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        coverUri TEXT,
                        bannerColorHex TEXT NOT NULL DEFAULT '#1E3A8A',
                        authorIdsJson TEXT NOT NULL DEFAULT '[]',
                        directBookIdsJson TEXT NOT NULL DEFAULT '[]',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS page_segment_explanations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bookId INTEGER NOT NULL,
                        pageNumber INTEGER NOT NULL,
                        segmentIndex INTEGER NOT NULL,
                        segmentText TEXT NOT NULL,
                        explanationSpanish TEXT NOT NULL DEFAULT '',
                        explanationEnglish TEXT NOT NULL DEFAULT '',
                        explanationFrench TEXT NOT NULL DEFAULT '',
                        explanationPortuguese TEXT NOT NULL DEFAULT '',
                        modelUsed TEXT NOT NULL DEFAULT 'gpt-4o-mini',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_page_segment_explanations_bookId_pageNumber ON page_segment_explanations(bookId, pageNumber)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_page_segment_explanations_bookId_pageNumber_segmentIndex ON page_segment_explanations(bookId, pageNumber, segmentIndex)")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS segment_chat_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bookId INTEGER NOT NULL,
                        pageNumber INTEGER NOT NULL,
                        segmentIndex INTEGER NOT NULL,
                        sender TEXT NOT NULL,
                        messageText TEXT NOT NULL,
                        isAudioInput INTEGER NOT NULL DEFAULT 0,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_segment_chat_messages_bookId_pageNumber_segmentIndex ON segment_chat_messages(bookId, pageNumber, segmentIndex)")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS saved_sentence_analyses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bookId INTEGER NOT NULL,
                        bookTitle TEXT NOT NULL,
                        pageNumber INTEGER NOT NULL,
                        languageCode TEXT NOT NULL,
                        sentenceText TEXT NOT NULL,
                        transliteration TEXT NOT NULL,
                        translationPt TEXT NOT NULL,
                        syntacticTreeSummary TEXT NOT NULL,
                        complexityLevel TEXT NOT NULL,
                        tokensJson TEXT NOT NULL,
                        grammarRulesJson TEXT NOT NULL,
                        c2MasteryTip TEXT NOT NULL,
                        practiceChallenge TEXT NOT NULL,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        userNotes TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_saved_sentence_analyses_bookId ON saved_sentence_analyses(bookId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_saved_sentence_analyses_languageCode ON saved_sentence_analyses(languageCode)")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS verb_phrasal_study_terms (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bookId INTEGER NOT NULL,
                        pageNumber INTEGER NOT NULL,
                        bookTitle TEXT NOT NULL,
                        verbOrPhrase TEXT NOT NULL,
                        isPhrasalVerb INTEGER NOT NULL,
                        prepositionUsed TEXT NOT NULL,
                        baseVerb TEXT NOT NULL,
                        grammaticalCategory TEXT NOT NULL,
                        languageCode TEXT NOT NULL,
                        definitionTargetLang TEXT NOT NULL,
                        portugueseMeaning TEXT NOT NULL,
                        ipaTranscription TEXT NOT NULL,
                        examplesJson TEXT NOT NULL,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        userNotes TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_verb_phrasal_study_terms_bookId_pageNumber ON verb_phrasal_study_terms(bookId, pageNumber)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_verb_phrasal_study_terms_verbOrPhrase ON verb_phrasal_study_terms(verbOrPhrase)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_verb_phrasal_study_terms_languageCode ON verb_phrasal_study_terms(languageCode)")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS public_organizations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        organizationType TEXT NOT NULL DEFAULT 'Órgão de Governo',
                        description TEXT NOT NULL DEFAULT '',
                        iconUri TEXT,
                        iconPreset TEXT NOT NULL DEFAULT 'DEFAULT',
                        accentColorHex TEXT NOT NULL DEFAULT '#0284C7',
                        pdfBookIdsJson TEXT NOT NULL DEFAULT '[]',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_public_organizations_name ON public_organizations(name)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "subtitle_sync_db"
                )
                    .addMigrations(
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
