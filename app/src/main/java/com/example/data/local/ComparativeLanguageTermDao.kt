package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ComparativeLanguageTerm
import kotlinx.coroutines.flow.Flow

@Dao
interface ComparativeLanguageTermDao {

    @Query("SELECT * FROM comparative_language_terms WHERE bookId = :bookId AND pageNumber = :pageNumber ORDER BY id ASC")
    fun getTermsForPage(bookId: Int, pageNumber: Int): Flow<List<ComparativeLanguageTerm>>

    @Query("SELECT * FROM comparative_language_terms WHERE bookId = :bookId AND pageNumber = :pageNumber ORDER BY id ASC")
    suspend fun getTermsForPageSync(bookId: Int, pageNumber: Int): List<ComparativeLanguageTerm>

    @Query("SELECT * FROM comparative_language_terms WHERE bookId = :bookId ORDER BY pageNumber ASC, id ASC")
    fun getTermsForBook(bookId: Int): Flow<List<ComparativeLanguageTerm>>

    @Query("SELECT * FROM comparative_language_terms ORDER BY createdAt DESC")
    fun getAllTerms(): Flow<List<ComparativeLanguageTerm>>

    @Query("""
        SELECT * FROM comparative_language_terms 
        WHERE term LIKE '%' || :query || '%' 
           OR meaning LIKE '%' || :query || '%'
           OR englishTerm LIKE '%' || :query || '%'
           OR frenchTerm LIKE '%' || :query || '%'
           OR spanishTerm LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchTerms(query: String): Flow<List<ComparativeLanguageTerm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerms(terms: List<ComparativeLanguageTerm>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(term: ComparativeLanguageTerm): Long

    @Update
    suspend fun update(term: ComparativeLanguageTerm)

    @Delete
    suspend fun delete(term: ComparativeLanguageTerm)

    @Query("DELETE FROM comparative_language_terms WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteTermsForPage(bookId: Int, pageNumber: Int)

    @Query("DELETE FROM comparative_language_terms WHERE bookId = :bookId")
    suspend fun deleteTermsForBook(bookId: Int)

    @Query("DELETE FROM comparative_language_terms")
    suspend fun clearAll()
}
