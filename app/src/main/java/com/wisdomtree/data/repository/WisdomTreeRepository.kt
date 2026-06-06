package com.wisdomtree.data.repository

import com.wisdomtree.data.db.WisdomTreeDatabase
import com.wisdomtree.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WisdomTreeRepository(private val db: WisdomTreeDatabase) {

    // ── Tree source ───────────────────────────────────────────────────────────

    suspend fun getSource(): String = withContext(Dispatchers.IO) {
        db.treeSourceDao().get()?.source ?: ""
    }

    suspend fun saveSource(src: String) = withContext(Dispatchers.IO) {
        db.treeSourceDao().save(TreeSourceEntity(source = src))
    }

    // ── Runs ──────────────────────────────────────────────────────────────────

    suspend fun getAllRuns(): List<RunEntity> = withContext(Dispatchers.IO) {
        db.runDao().getAll()
    }

    suspend fun getRunsSince(days: Int): List<RunEntity> = withContext(Dispatchers.IO) {
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        db.runDao().getSince(since)
    }

    suspend fun saveRun(run: RunEntity) = withContext(Dispatchers.IO) {
        db.runDao().insert(run)
    }

    suspend fun deleteRun(id: String) = withContext(Dispatchers.IO) {
        db.runDao().delete(id)
    }

    // ── Var overrides ─────────────────────────────────────────────────────────

    suspend fun getVarOverrides(): Map<String, String> = withContext(Dispatchers.IO) {
        db.varOverrideDao().getAll().associate { it.key to it.value }
    }

    suspend fun setVarOverride(key: String, value: String) = withContext(Dispatchers.IO) {
        db.varOverrideDao().set(VarOverrideEntity(key, value))
    }

    // ── Analytics helpers ─────────────────────────────────────────────────────

    /** Compute global vars and node stats from all runs */
    fun computeGlobalStats(runs: List<RunEntity>): GlobalStats {
        val vars = mutableMapOf<String, String>()
        val nodeStats = mutableMapOf<String, NodeStat>()

        runs.forEach { run ->
            // Aggregate vars
            run.vars.forEach { (k, v) -> vars[k] = v }
            // Aggregate node answers
            run.steps.forEach { step ->
                val stat = nodeStats.getOrPut(step.nodeId) { NodeStat() }
                when (step.answer?.type) {
                    "single" -> step.answer.label?.let { stat.options[it] = (stat.options[it] ?: 0) + 1 }
                    "multi" -> step.answer.selected?.forEach { l ->
                        stat.options[l] = (stat.options[l] ?: 0) + 1
                    }
                    "text" -> step.answer.text?.let {
                        stat.texts.add(TextEntry(it, step.timestamp))
                    }
                }
            }
        }
        return GlobalStats(vars, nodeStats)
    }
}

data class GlobalStats(
    val vars: Map<String, String>,
    val nodeStats: Map<String, NodeStat>
)

data class NodeStat(
    val options: MutableMap<String, Int> = mutableMapOf(),
    val texts: MutableList<TextEntry> = mutableListOf()
)

data class TextEntry(val text: String, val timestamp: Long)
