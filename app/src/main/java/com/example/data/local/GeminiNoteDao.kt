package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.GeminiNote
import kotlinx.coroutines.flow.Flow

@Dao
interface GeminiNoteDao {
    @Query("SELECT * FROM gemini_notes WHERE projectId = :projectId ORDER BY cueIndex ASC, createdAt DESC")
    fun getNotesForProject(projectId: Int): Flow<List<GeminiNote>>

    @Query("SELECT * FROM gemini_notes WHERE projectId = :projectId AND cueIndex = :cueIndex LIMIT 1")
    suspend fun getNoteForCue(projectId: Int, cueIndex: Int): GeminiNote?

    @Query("SELECT * FROM gemini_notes WHERE termOrSentence = :termOrSentence LIMIT 1")
    suspend fun getNoteByTerm(termOrSentence: String): GeminiNote?

    @Query("SELECT * FROM gemini_notes WHERE projectId = :projectId AND termOrSentence = :termOrSentence LIMIT 1")
    suspend fun getNoteByTermAndProject(projectId: Int, termOrSentence: String): GeminiNote?

    @Query("SELECT * FROM gemini_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<GeminiNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNote(note: GeminiNote): Long

    @Delete
    suspend fun deleteNote(note: GeminiNote)

    @Query("DELETE FROM gemini_notes WHERE projectId = :projectId AND cueIndex = :cueIndex")
    suspend fun deleteNoteForCue(projectId: Int, cueIndex: Int)

    @Query("DELETE FROM gemini_notes WHERE projectId = :projectId")
    suspend fun deleteNotesForProject(projectId: Int)
}
