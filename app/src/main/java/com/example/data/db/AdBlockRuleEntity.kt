package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "adblock_rules")
data class AdBlockRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rulePattern: String,
    val domain: String = "",
    val ruleType: String = "BLOCK", // BLOCK, ALLOW, TRACKER, ELEMENT_HIDE
    val isCustom: Boolean = false,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface AdBlockRuleDao {
    @Query("SELECT * FROM adblock_rules WHERE isEnabled = 1")
    fun getActiveRules(): Flow<List<AdBlockRuleEntity>>

    @Query("SELECT * FROM adblock_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<AdBlockRuleEntity>>

    @Query("SELECT * FROM adblock_rules WHERE ruleType = 'ALLOW'")
    fun getAllowlist(): Flow<List<AdBlockRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AdBlockRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<AdBlockRuleEntity>)

    @Delete
    suspend fun deleteRule(rule: AdBlockRuleEntity)

    @Query("DELETE FROM adblock_rules WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)

    @Query("UPDATE adblock_rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setRuleEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM adblock_rules WHERE isCustom = 1")
    suspend fun clearCustomRules()
}
