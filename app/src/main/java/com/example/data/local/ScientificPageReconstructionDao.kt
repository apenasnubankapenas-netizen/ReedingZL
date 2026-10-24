package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScientificPageReconstruction
import kotlinx.coroutines.flow.Flow

@Dao
interface ScientificPageReconstructionDao {

    @Query("SELECT * FROM scientific_page_reconstructions WHERE bookId = :bookId AND pageNumber = :pageNumber LIMIT 1")
    fun getReconstructionForPage(bookId: Int, pageNumber: Int): Flow<ScientificPageReconstruction?>

    @Query("SELECT * FROM scientific_page_reconstructions WHERE bookId = :bookId AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getReconstructionForPageSync(bookId: Int, pageNumber: Int): ScientificPageReconstruction?

    @Query("SELECT * FROM scientific_page_reconstructions WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getReconstructionsForBook(bookId: Int): Flow<List<ScientificPageReconstruction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(reconstruction: ScientificPageReconstruction): Long

    @Update
    suspend fun update(reconstruction: ScientificPageReconstruction)

    @Delete
    suspend fun delete(reconstruction: ScientificPageReconstruction)

    @Query("DELETE FROM scientific_page_reconstructions WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteForPage(bookId: Int, pageNumber: Int)
}
