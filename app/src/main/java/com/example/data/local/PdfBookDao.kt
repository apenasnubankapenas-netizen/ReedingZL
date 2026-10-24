package com.example.data.local

import androidx.room.*
import com.example.data.model.PdfBook
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfBookDao {
    @Query("SELECT * FROM pdf_books ORDER BY id ASC")
    fun getAllPdfBooks(): Flow<List<PdfBook>>

    @Query("SELECT * FROM pdf_books ORDER BY dateAdded DESC")
    fun getAllBooks(): Flow<List<PdfBook>>

    @Query("SELECT * FROM pdf_books WHERE id = :bookId LIMIT 1")
    suspend fun getBookById(bookId: Int): PdfBook?

    @Query("SELECT * FROM pdf_books WHERE id = :bookId LIMIT 1")
    fun getBookByIdFlow(bookId: Int): Flow<PdfBook?>

    @Query("SELECT * FROM pdf_books WHERE authorCollectionId = :authorId ORDER BY id ASC")
    fun getBooksByAuthorId(authorId: Int): Flow<List<PdfBook>>

    @Query("SELECT * FROM pdf_books WHERE authorCollectionId IS NULL ORDER BY id ASC")
    fun getStandaloneBooks(): Flow<List<PdfBook>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(book: PdfBook): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: PdfBook)

    @Update
    suspend fun update(book: PdfBook)

    @Update
    suspend fun updateBook(book: PdfBook)

    @Query("UPDATE pdf_books SET currentPage = :page WHERE id = :bookId")
    suspend fun updateProgress(bookId: Int, page: Int)

    @Query("UPDATE pdf_books SET speechSpeed = :speed WHERE id = :bookId")
    suspend fun updateSpeechSpeed(bookId: Int, speed: Float)

    @Query("UPDATE pdf_books SET readingTheme = :theme WHERE id = :bookId")
    suspend fun updateReadingTheme(bookId: Int, theme: String)

    @Query("UPDATE pdf_books SET readingTheme = :theme")
    suspend fun updateAllReadingThemes(theme: String)

    @Query("UPDATE pdf_books SET starredPagesJson = :starredJson WHERE id = :bookId")
    suspend fun updateStarredPages(bookId: Int, starredJson: String)

    @Query("UPDATE pdf_books SET category = :category, subcategory = :subcategory, categoryColor = :color WHERE id = :bookId")
    suspend fun updateCategory(bookId: Int, category: String?, subcategory: String?, color: String?)

    @Query("UPDATE pdf_books SET authorCollectionId = :authorId WHERE id = :bookId")
    suspend fun setBookAuthorCollection(bookId: Int, authorId: Int?)

    @Query("UPDATE pdf_books SET authorCollectionId = NULL WHERE authorCollectionId = :authorId")
    suspend fun clearAuthorFromBooks(authorId: Int)

    @Delete
    suspend fun delete(book: PdfBook)

    @Delete
    suspend fun deleteBook(book: PdfBook)

    @Query("DELETE FROM pdf_books WHERE id = :id")
    suspend fun deleteById(id: Int)
}
