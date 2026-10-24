package com.example.data.local

import androidx.room.*
import com.example.data.model.BookHighlight
import kotlinx.coroutines.flow.Flow

@Dao
interface BookHighlightDao {
    @Query("SELECT * FROM book_highlights ORDER BY createdAt DESC")
    fun getAllHighlights(): Flow<List<BookHighlight>>

    @Query("SELECT * FROM book_highlights WHERE bookId = :bookId ORDER BY pageNumber ASC, createdAt DESC")
    fun getHighlightsForBook(bookId: Int): Flow<List<BookHighlight>>

    @Query("SELECT * FROM book_highlights WHERE bookId = :bookId AND pageNumber = :pageNumber ORDER BY createdAt DESC")
    fun getHighlightsForPage(bookId: Int, pageNumber: Int): Flow<List<BookHighlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: BookHighlight): Long

    @Update
    suspend fun updateHighlight(highlight: BookHighlight)

    @Delete
    suspend fun deleteHighlight(highlight: BookHighlight)

    @Query("DELETE FROM book_highlights WHERE id = :id")
    suspend fun deleteById(id: Int)
}
