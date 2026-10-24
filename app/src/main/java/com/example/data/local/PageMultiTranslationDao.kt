package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PageMultiTranslation
import kotlinx.coroutines.flow.Flow

@Dao
interface PageMultiTranslationDao {

    @Query("SELECT * FROM page_multi_translations WHERE bookId = :bookId AND pageNumber = :pageNumber LIMIT 1")
    fun getTranslationForPage(bookId: Int, pageNumber: Int): Flow<PageMultiTranslation?>

    @Query("SELECT * FROM page_multi_translations WHERE bookId = :bookId AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getTranslationForPageSync(bookId: Int, pageNumber: Int): PageMultiTranslation?

    @Query("SELECT * FROM page_multi_translations WHERE bookId = :bookId")
    fun getTranslationsForBook(bookId: Int): Flow<List<PageMultiTranslation>>

    @Query("SELECT * FROM page_multi_translations")
    fun getAllTranslations(): Flow<List<PageMultiTranslation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(translation: PageMultiTranslation): Long

    @Update
    suspend fun update(translation: PageMultiTranslation)

    @Delete
    suspend fun delete(translation: PageMultiTranslation)

    @Query("DELETE FROM page_multi_translations WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteForPage(bookId: Int, pageNumber: Int)

    @Query("DELETE FROM page_multi_translations WHERE bookId = :bookId")
    suspend fun deleteAllForBook(bookId: Int)
}
