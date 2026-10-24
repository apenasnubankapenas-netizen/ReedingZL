package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.OpenAiPageSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface OpenAiPageSummaryDao {

    @Query("SELECT * FROM openai_page_summaries WHERE bookId = :bookId AND pageNumber = :pageNumber LIMIT 1")
    fun getSummaryForPage(bookId: Int, pageNumber: Int): Flow<OpenAiPageSummary?>

    @Query("SELECT * FROM openai_page_summaries WHERE bookId = :bookId AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getSummaryForPageSync(bookId: Int, pageNumber: Int): OpenAiPageSummary?

    @Query("SELECT * FROM openai_page_summaries WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getAllSummariesForBook(bookId: Int): Flow<List<OpenAiPageSummary>>

    @Query("SELECT * FROM openai_page_summaries ORDER BY createdAt DESC")
    fun getAllSummaries(): Flow<List<OpenAiPageSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(summary: OpenAiPageSummary): Long

    @Update
    suspend fun update(summary: OpenAiPageSummary)

    @Delete
    suspend fun delete(summary: OpenAiPageSummary)

    @Query("DELETE FROM openai_page_summaries WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteByBookAndPage(bookId: Int, pageNumber: Int)

    @Query("DELETE FROM openai_page_summaries WHERE bookId = :bookId")
    suspend fun deleteAllForBook(bookId: Int)
}
