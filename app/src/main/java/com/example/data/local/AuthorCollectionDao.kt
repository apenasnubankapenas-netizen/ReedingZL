package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AuthorCollection
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthorCollectionDao {
    @Query("SELECT * FROM author_collections ORDER BY name ASC")
    fun getAllAuthors(): Flow<List<AuthorCollection>>

    @Query("SELECT * FROM author_collections WHERE id = :id LIMIT 1")
    fun getAuthorById(id: Int): Flow<AuthorCollection?>

    @Query("SELECT * FROM author_collections WHERE id = :id LIMIT 1")
    suspend fun getAuthorByIdSync(id: Int): AuthorCollection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(author: AuthorCollection): Long

    @Update
    suspend fun update(author: AuthorCollection)

    @Delete
    suspend fun delete(author: AuthorCollection)

    @Query("DELETE FROM author_collections WHERE id = :id")
    suspend fun deleteById(id: Int)
}
