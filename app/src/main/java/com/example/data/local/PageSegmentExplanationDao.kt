package com.example.data.local

import androidx.room.*
import com.example.data.model.PageSegmentExplanation
import kotlinx.coroutines.flow.Flow

@Dao
interface PageSegmentExplanationDao {
    @Query("SELECT * FROM page_segment_explanations ORDER BY bookId, pageNumber, segmentIndex ASC")
    fun getAllExplanations(): Flow<List<PageSegmentExplanation>>

    @Query("SELECT * FROM page_segment_explanations WHERE bookId = :bookId AND pageNumber = :page ORDER BY segmentIndex ASC")
    fun getExplanationsForPage(bookId: Int, page: Int): Flow<List<PageSegmentExplanation>>

    @Query("SELECT * FROM page_segment_explanations WHERE bookId = :bookId AND pageNumber = :page ORDER BY segmentIndex ASC")
    suspend fun getExplanationsForPageSync(bookId: Int, page: Int): List<PageSegmentExplanation>

    @Query("SELECT * FROM page_segment_explanations WHERE bookId = :bookId ORDER BY pageNumber, segmentIndex ASC")
    fun getAllExplanationsForBook(bookId: Int): Flow<List<PageSegmentExplanation>>

    @Query("SELECT * FROM page_segment_explanations WHERE bookId = :bookId AND pageNumber = :page AND segmentIndex = :segmentIndex LIMIT 1")
    suspend fun getExplanationForSegment(bookId: Int, page: Int, segmentIndex: Int): PageSegmentExplanation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(explanation: PageSegmentExplanation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(explanations: List<PageSegmentExplanation>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExplanation(explanation: PageSegmentExplanation): Long

    @Delete
    suspend fun delete(explanation: PageSegmentExplanation)

    @Delete
    suspend fun deleteExplanation(explanation: PageSegmentExplanation)

    @Query("DELETE FROM page_segment_explanations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM page_segment_explanations WHERE bookId = :bookId AND pageNumber = :page AND segmentIndex = :segmentIndex")
    suspend fun deleteForSegment(bookId: Int, page: Int, segmentIndex: Int)

    @Query("DELETE FROM page_segment_explanations WHERE bookId = :bookId AND pageNumber = :page")
    suspend fun deleteForPage(bookId: Int, page: Int)

    @Query("DELETE FROM page_segment_explanations WHERE bookId = :bookId")
    suspend fun deleteAllForBook(bookId: Int)
}
