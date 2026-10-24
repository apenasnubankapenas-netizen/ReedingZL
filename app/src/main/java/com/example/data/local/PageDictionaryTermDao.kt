package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PageDictionaryTerm
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDictionaryTermDao {

    @Query("SELECT * FROM page_dictionary_terms WHERE bookId = :bookId AND pageNumber = :pageNumber ORDER BY id ASC")
    fun getTermsForPage(bookId: Int, pageNumber: Int): Flow<List<PageDictionaryTerm>>

    @Query("SELECT * FROM page_dictionary_terms WHERE bookId = :bookId AND pageNumber = :pageNumber ORDER BY id ASC")
    suspend fun getTermsForPageSync(bookId: Int, pageNumber: Int): List<PageDictionaryTerm>

    @Query("SELECT * FROM page_dictionary_terms WHERE bookId = :bookId ORDER BY pageNumber ASC, id ASC")
    fun getTermsForBook(bookId: Int): Flow<List<PageDictionaryTerm>>

    @Query("SELECT * FROM page_dictionary_terms ORDER BY createdAt DESC")
    fun getAllTerms(): Flow<List<PageDictionaryTerm>>

    @Query("SELECT * FROM page_dictionary_terms ORDER BY createdAt DESC")
    suspend fun getAllTermsSync(): List<PageDictionaryTerm>

    @Query("SELECT * FROM page_dictionary_terms WHERE LOWER(TRIM(term)) = LOWER(TRIM(:termName)) LIMIT 1")
    suspend fun findTermByName(termName: String): PageDictionaryTerm?

    @Query("""
        SELECT * FROM page_dictionary_terms 
        WHERE term LIKE '%' || :query || '%' 
           OR originalDefinition LIKE '%' || :query || '%'
           OR spanishTerm LIKE '%' || :query || '%'
           OR frenchTerm LIKE '%' || :query || '%'
           OR englishTerm LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchTerms(query: String): Flow<List<PageDictionaryTerm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerms(terms: List<PageDictionaryTerm>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(term: PageDictionaryTerm): Long

    @Update
    suspend fun update(term: PageDictionaryTerm)

    @Delete
    suspend fun delete(term: PageDictionaryTerm)

    @Query("DELETE FROM page_dictionary_terms WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteTermsForPage(bookId: Int, pageNumber: Int)

    @Query("DELETE FROM page_dictionary_terms WHERE bookId = :bookId")
    suspend fun deleteTermsForBook(bookId: Int)

    @Query("DELETE FROM page_dictionary_terms")
    suspend fun clearAll()
}
