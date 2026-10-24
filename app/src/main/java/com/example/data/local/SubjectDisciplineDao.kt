package com.example.data.local

import androidx.room.*
import com.example.data.model.SubjectDiscipline
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDisciplineDao {
    @Query("SELECT * FROM subject_disciplines ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectDiscipline>>

    @Query("SELECT * FROM subject_disciplines ORDER BY name ASC")
    fun getAllDisciplines(): Flow<List<SubjectDiscipline>>

    @Query("SELECT * FROM subject_disciplines WHERE id = :id LIMIT 1")
    fun getSubjectById(id: Int): Flow<SubjectDiscipline?>

    @Query("SELECT * FROM subject_disciplines WHERE id = :id LIMIT 1")
    suspend fun getSubjectByIdSync(id: Int): SubjectDiscipline?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(discipline: SubjectDiscipline): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscipline(discipline: SubjectDiscipline): Long

    @Update
    suspend fun update(discipline: SubjectDiscipline)

    @Delete
    suspend fun delete(discipline: SubjectDiscipline)

    @Delete
    suspend fun deleteDiscipline(discipline: SubjectDiscipline)

    @Query("DELETE FROM subject_disciplines WHERE id = :id")
    suspend fun deleteById(id: Int)
}
