package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BookQuestion
import kotlinx.coroutines.flow.Flow

@Dao
interface BookQuestionDao {

    @Query("SELECT * FROM book_questions WHERE bookId = :bookId ORDER BY startPage ASC, id ASC")
    fun getQuestionsForBook(bookId: Int): Flow<List<BookQuestion>>

    @Query("SELECT * FROM book_questions WHERE bookId = :bookId AND startPage <= :pageNumber AND endPage >= :pageNumber ORDER BY id ASC")
    fun getQuestionsForPage(bookId: Int, pageNumber: Int): Flow<List<BookQuestion>>

    @Query("SELECT * FROM book_questions WHERE bookId = :bookId AND startPage <= :pageNumber AND endPage >= :pageNumber ORDER BY id ASC")
    suspend fun getQuestionsForPageSync(bookId: Int, pageNumber: Int): List<BookQuestion>

    @Query("SELECT * FROM book_questions ORDER BY id ASC")
    fun getAllQuestions(): Flow<List<BookQuestion>>

    @Insert
    suspend fun insertQuestion(question: BookQuestion): Long

    @Insert
    suspend fun insertQuestions(questions: List<BookQuestion>): List<Long>

    @Update
    suspend fun updateQuestion(question: BookQuestion)

    @Query("UPDATE book_questions SET userAnswerIndex = :userAnswerIndex, isAnswered = 1, isCorrect = :isCorrect WHERE id = :id")
    suspend fun recordAnswer(id: Long, userAnswerIndex: Int, isCorrect: Boolean)

    @Query("UPDATE book_questions SET userAnswerIndex = NULL, isAnswered = 0, isCorrect = NULL WHERE id = :id")
    suspend fun resetAnswer(id: Long)

    @Query("UPDATE book_questions SET userAnswerIndex = NULL, isAnswered = 0, isCorrect = NULL WHERE bookId = :bookId")
    suspend fun resetAllAnswersForBook(bookId: Int)

    @Query("UPDATE book_questions SET userAnswerIndex = NULL, isAnswered = 0, isCorrect = NULL")
    suspend fun resetAllAnswersGlobal()

    @Query("DELETE FROM book_questions WHERE id = :id")
    suspend fun deleteQuestionById(id: Long)

    @Query("DELETE FROM book_questions WHERE bookId = :bookId AND startPage = :page AND endPage = :page")
    suspend fun deleteQuestionsForPage(bookId: Int, page: Int)

    @Query("DELETE FROM book_questions WHERE bookId = :bookId")
    suspend fun deleteQuestionsForBook(bookId: Int)
}
