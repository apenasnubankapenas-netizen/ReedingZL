package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SavedSentenceAnalysis
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSentenceAnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: SavedSentenceAnalysis): Long

    @Update
    suspend fun update(analysis: SavedSentenceAnalysis)

    @Delete
    suspend fun delete(analysis: SavedSentenceAnalysis)

    @Query("DELETE FROM saved_sentence_analyses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM saved_sentence_analyses WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getByBook(bookId: Int): Flow<List<SavedSentenceAnalysis>>

    @Query("SELECT * FROM saved_sentence_analyses WHERE languageCode = :langCode ORDER BY createdAt DESC")
    fun getByLanguage(langCode: String): Flow<List<SavedSentenceAnalysis>>

    @Query("SELECT * FROM saved_sentence_analyses ORDER BY createdAt DESC")
    fun getAll(): Flow<List<SavedSentenceAnalysis>>

    @Query("SELECT * FROM saved_sentence_analyses WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavorites(): Flow<List<SavedSentenceAnalysis>>
}
