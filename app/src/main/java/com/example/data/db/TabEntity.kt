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

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey
    val tabId: String,
    val title: String,
    val url: String,
    val isDesktopMode: Boolean = false,
    val groupName: String? = null,
    val groupColor: Int? = null,
    val isSuspended: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessed: Long = System.currentTimeMillis(),
    val position: Int = 0
)

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY position ASC, lastAccessed DESC")
    fun getAllTabs(): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE tabId = :tabId LIMIT 1")
    suspend fun getTabById(tabId: String): TabEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(tab: TabEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tabs: List<TabEntity>)

    @Update
    suspend fun updateTab(tab: TabEntity)

    @Delete
    suspend fun deleteTab(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE tabId = :tabId")
    suspend fun deleteById(tabId: String)

    @Query("DELETE FROM tabs")
    suspend fun clearAll()
}
