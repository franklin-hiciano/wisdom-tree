package com.wisdomtree.data.model

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// ── Type converters ───────────────────────────────────────────────────────────

class Converters {
    private val gson = Gson()

    @TypeConverter fun stepListToJson(v: List<RunStep>): String = gson.toJson(v)
    @TypeConverter fun jsonToStepList(v: String): List<RunStep> =
        gson.fromJson(v, object : TypeToken<List<RunStep>>() {}.type)

    @TypeConverter fun mapToJson(v: Map<String, String>): String = gson.toJson(v)
    @TypeConverter fun jsonToMap(v: String): Map<String, String> =
        gson.fromJson(v, object : TypeToken<Map<String, String>>() {}.type) ?: emptyMap()
}

// ── Entities ──────────────────────────────────────────────────────────────────

@Entity(tableName = "tree_source")
data class TreeSourceEntity(
    @PrimaryKey val id: Int = 1,
    val source: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class RunStep(
    val nodeId: String,
    val next: String?,
    val answer: RunAnswer?,
    val timestamp: Long = System.currentTimeMillis()
)

data class RunAnswer(
    val type: String,       // "single" | "multi" | "text"
    val label: String? = null,
    val selected: List<String>? = null,
    val text: String? = null
)

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey val runId: String,
    val savedAt: Long,
    val steps: List<RunStep>,
    val vars: Map<String, String>
)

@Entity(tableName = "var_overrides")
data class VarOverrideEntity(
    @PrimaryKey val key: String,
    val value: String
)

// ── DAOs ──────────────────────────────────────────────────────────────────────

@Dao
interface TreeSourceDao {
    @Query("SELECT * FROM tree_source WHERE id = 1")
    suspend fun get(): TreeSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: TreeSourceEntity)
}

@Dao
interface RunDao {
    @Query("SELECT * FROM runs ORDER BY savedAt DESC")
    suspend fun getAll(): List<RunEntity>

    @Query("SELECT * FROM runs WHERE savedAt > :since ORDER BY savedAt DESC")
    suspend fun getSince(since: Long): List<RunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(run: RunEntity)

    @Query("DELETE FROM runs WHERE runId = :id")
    suspend fun delete(id: String)
}

@Dao
interface VarOverrideDao {
    @Query("SELECT * FROM var_overrides")
    suspend fun getAll(): List<VarOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(v: VarOverrideEntity)

    @Query("DELETE FROM var_overrides WHERE key = :key")
    suspend fun delete(key: String)
}
