package com.example.data.local

import androidx.room.*
import com.example.data.model.PublicOrganization
import kotlinx.coroutines.flow.Flow

@Dao
interface PublicOrganizationDao {
    @Query("SELECT * FROM public_organizations ORDER BY name ASC")
    fun getAllOrganizations(): Flow<List<PublicOrganization>>

    @Query("SELECT * FROM public_organizations WHERE id = :id LIMIT 1")
    suspend fun getOrganizationByIdSync(id: Int): PublicOrganization?

    @Query("SELECT * FROM public_organizations WHERE id = :id LIMIT 1")
    fun getOrganizationById(id: Int): Flow<PublicOrganization?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(org: PublicOrganization): Long

    @Update
    suspend fun update(org: PublicOrganization)

    @Delete
    suspend fun delete(org: PublicOrganization)
}
