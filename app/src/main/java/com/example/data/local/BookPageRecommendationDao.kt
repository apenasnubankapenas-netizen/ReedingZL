package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BookPageRecommendation
import kotlinx.coroutines.flow.Flow

@Dao
interface BookPageRecommendationDao {
    @Query("SELECT * FROM book_page_recommendations ORDER BY pageNumber ASC, id ASC")
    fun getAllRecommendations(): Flow<List<BookPageRecommendation>>

    @Query("SELECT * FROM book_page_recommendations WHERE bookId = :bookId AND pageNumber = :pageNumber ORDER BY id ASC")
    fun getRecommendationsForPage(bookId: Int, pageNumber: Int): Flow<List<BookPageRecommendation>>

    @Query("SELECT * FROM book_page_recommendations WHERE bookId = :bookId AND pageNumber = :pageNumber ORDER BY id ASC")
    suspend fun getRecommendationsForPageSync(bookId: Int, pageNumber: Int): List<BookPageRecommendation>

    @Query("SELECT * FROM book_page_recommendations WHERE bookId = :bookId ORDER BY pageNumber ASC, id ASC")
    fun getAllRecommendationsForBook(bookId: Int): Flow<List<BookPageRecommendation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recommendations: List<BookPageRecommendation>): List<Long>

    @Query("DELETE FROM book_page_recommendations WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteForPage(bookId: Int, pageNumber: Int)

    @Query("DELETE FROM book_page_recommendations WHERE bookId = :bookId")
    suspend fun deleteAllForBook(bookId: Int)
}
