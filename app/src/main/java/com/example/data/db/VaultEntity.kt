package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vault_credentials")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val siteDomain: String,
    val username: String,
    val encryptedPasswordBase64: String,
    val initializationVectorBase64: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis()
)

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_credentials ORDER BY siteDomain ASC")
    fun getAllCredentials(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_credentials WHERE siteDomain LIKE '%' || :domain || '%'")
    suspend fun getCredentialsForDomain(domain: String): List<VaultEntity>

    @Query("SELECT * FROM vault_credentials WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): VaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(item: VaultEntity): Long

    @Update
    suspend fun updateCredential(item: VaultEntity)

    @Delete
    suspend fun deleteCredential(item: VaultEntity)

    @Query("DELETE FROM vault_credentials WHERE id = :id")
    suspend fun deleteById(id: Long)
}
