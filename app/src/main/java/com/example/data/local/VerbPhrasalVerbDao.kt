package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VerbPhrasalVerbStudyTerm
import kotlinx.coroutines.flow.Flow

@Dao
interface VerbPhrasalVerbDao {

    @Query("SELECT * FROM verb_phrasal_study_terms WHERE bookId = :bookId AND pageNumber = :pageNumber ORDER BY isPhrasalVerb ASC, verbOrPhrase ASC")
    fun getTermsForPage(bookId: Int, pageNumber: Int): Flow<List<VerbPhrasalVerbStudyTerm>>

    @Query("SELECT * FROM verb_phrasal_study_terms WHERE bookId = :bookId ORDER BY pageNumber ASC, isPhrasalVerb ASC, verbOrPhrase ASC")
    fun getTermsForBook(bookId: Int): Flow<List<VerbPhrasalVerbStudyTerm>>

    @Query("SELECT * FROM verb_phrasal_study_terms ORDER BY createdAt DESC")
    fun getAllTerms(): Flow<List<VerbPhrasalVerbStudyTerm>>

    @Query("SELECT * FROM verb_phrasal_study_terms WHERE languageCode = :languageCode ORDER BY createdAt DESC")
    fun getTermsByLanguage(languageCode: String): Flow<List<VerbPhrasalVerbStudyTerm>>

    @Query("""
        SELECT * FROM verb_phrasal_study_terms 
        WHERE verbOrPhrase LIKE '%' || :query || '%' 
           OR portugueseMeaning LIKE '%' || :query || '%' 
           OR prepositionUsed LIKE '%' || :query || '%'
           OR definitionTargetLang LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchTerms(query: String): Flow<List<VerbPhrasalVerbStudyTerm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerms(terms: List<VerbPhrasalVerbStudyTerm>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(term: VerbPhrasalVerbStudyTerm): Long

    @Update
    suspend fun update(term: VerbPhrasalVerbStudyTerm)

    @Delete
    suspend fun delete(term: VerbPhrasalVerbStudyTerm)

    @Query("DELETE FROM verb_phrasal_study_terms WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteTermsForPage(bookId: Int, pageNumber: Int)

    @Query("DELETE FROM verb_phrasal_study_terms WHERE bookId = :bookId")
    suspend fun deleteTermsForBook(bookId: Int)

    @Query("UPDATE verb_phrasal_study_terms SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)

    @Query("UPDATE verb_phrasal_study_terms SET userNotes = :notes WHERE id = :id")
    suspend fun updateUserNotes(id: Int, notes: String)

    @Query("DELETE FROM verb_phrasal_study_terms")
    suspend fun clearAll()
}
