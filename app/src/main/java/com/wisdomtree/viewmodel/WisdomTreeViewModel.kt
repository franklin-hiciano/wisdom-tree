package com.wisdomtree.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wisdomtree.data.db.WisdomTreeDatabase
import com.wisdomtree.data.model.*
import com.wisdomtree.data.repository.GlobalStats
import com.wisdomtree.data.repository.WisdomTreeRepository
import com.wisdomtree.parser.TreeNode
import com.wisdomtree.parser.WisdomTreeParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── UI state ──────────────────────────────────────────────────────────────────

data class EditorState(
    val source: String = "",
    val nodeCount: Int = 0,
    val errors: List<String> = emptyList(),
    val selectedNodeId: String? = null
)

data class CanvasState(
    val nodes: Map<String, TreeNode> = emptyMap(),
    val selectedNodeId: String? = null
)

data class TestState(
    val active: Boolean = false,
    val currentNodeId: String? = null,
    val history: List<HistoryEntry> = emptyList(),   // for back navigation
    val vars: MutableMap<String, Any> = mutableMapOf(),
    val showSaveBanner: Boolean = false
)

data class HistoryEntry(
    val nodeId: String,
    val answer: RunAnswer?,
    val nextId: String?,
    val vars: Map<String, Any>
)

data class RunsState(
    val runs: List<RunEntity> = emptyList(),
    val filter: String = "7d"   // "7d" | "30d" | "all"
)

data class VarsState(
    val stats: GlobalStats? = null,
    val overrides: Map<String, String> = emptyMap()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class WisdomTreeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WisdomTreeRepository(WisdomTreeDatabase.getInstance(app))

    // Parsed tree (recomputed whenever source changes)
    private var _parsedNodes: Map<String, TreeNode> = emptyMap()
    private var _parseErrors: List<String> = emptyList()

    // ── Editor ────────────────────────────────────────────────────────────────
    private val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    // ── Canvas ────────────────────────────────────────────────────────────────
    private val _canvas = MutableStateFlow(CanvasState())
    val canvas: StateFlow<CanvasState> = _canvas.asStateFlow()

    // ── Test ──────────────────────────────────────────────────────────────────
    private val _test = MutableStateFlow(TestState())
    val test: StateFlow<TestState> = _test.asStateFlow()

    // ── Runs ──────────────────────────────────────────────────────────────────
    private val _runs = MutableStateFlow(RunsState())
    val runs: StateFlow<RunsState> = _runs.asStateFlow()

    // ── Vars ──────────────────────────────────────────────────────────────────
    private val _vars = MutableStateFlow(VarsState())
    val vars: StateFlow<VarsState> = _vars.asStateFlow()

    init {
        viewModelScope.launch {
            val src = repo.getSource()
            updateSource(src)
        }
    }

    // ── Source editing ────────────────────────────────────────────────────────

    fun updateSource(src: String) {
        viewModelScope.launch {
            repo.saveSource(src)
        }
        val result = WisdomTreeParser.parse(src)
        _parsedNodes = result.nodes
        _parseErrors = result.errors
        _editor.value = EditorState(
            source = src,
            nodeCount = result.nodes.size,
            errors = result.errors,
            selectedNodeId = _editor.value.selectedNodeId
        )
        _canvas.value = _canvas.value.copy(nodes = result.nodes)
    }

    fun selectNode(id: String?) {
        _editor.value = _editor.value.copy(selectedNodeId = id)
        _canvas.value = _canvas.value.copy(selectedNodeId = id)
    }

    fun resetSource() {
        updateSource("")
    }

    // ── Test mode ─────────────────────────────────────────────────────────────

    fun enterTest() {
        val firstNode = _parsedNodes.keys.firstOrNull() ?: return
        _test.value = TestState(
            active = true,
            currentNodeId = firstNode,
            history = emptyList(),
            vars = mutableMapOf()
        )
    }

    fun exitTest() {
        _test.value = TestState(active = false)
    }

    fun answerSingle(node: TreeNode, optionIndex: Int) {
        val opt = node.opts[optionIndex]
        val answer = RunAnswer(type = "single", label = opt.label)
        advanceTest(node, opt.next ?: node.def, answer)
    }

    fun answerMulti(node: TreeNode, selected: List<String>) {
        val answer = RunAnswer(type = "multi", selected = selected)
        advanceTest(node, node.def, answer)
    }

    fun answerText(node: TreeNode, text: String) {
        val answer = RunAnswer(type = "text", text = text)
        advanceTest(node, node.def, answer)
    }

    private fun advanceTest(node: TreeNode, nextId: String?, answer: RunAnswer?) {
        // Apply @set actions
        val vars = _test.value.vars.toMutableMap()
        node.sets.forEach { set ->
            val cur = (vars[set.key] as? Number)?.toDouble() ?: 0.0
            val rhs = (set.value as? Number)?.toDouble() ?: set.value.toString().toDoubleOrNull() ?: 0.0
            vars[set.key] = when (set.op) {
                "+=" -> cur + rhs
                "-=" -> cur - rhs
                "*=" -> cur * rhs
                "/=" -> if (rhs != 0.0) cur / rhs else 0.0
                else -> set.value
            }
        }

        val entry = HistoryEntry(
            nodeId = node.id,
            answer = answer,
            nextId = nextId,
            vars = _test.value.vars.toMap() // Snapshot vars BEFORE applying new ones
        )
        val newHistory = _test.value.history + entry

        // Resolve @if conditions to override nextId
        val resolvedNext = resolveConditions(node, vars) ?: nextId

        _test.value = _test.value.copy(
            currentNodeId = resolvedNext,
            history = newHistory,
            vars = vars
        )
    }

    private fun resolveConditions(node: TreeNode, vars: Map<String, Any>): String? {
        node.conditions.forEach { cond ->
            val rawVar = vars[cond.key] ?: return@forEach
            val varValNum = (rawVar as? Number)?.toDouble() ?: rawVar.toString().toDoubleOrNull()
            val condValNum = (cond.value as? Number)?.toDouble() ?: cond.value.toString().toDoubleOrNull()

            val matches = if (varValNum != null && condValNum != null) {
                // Numeric comparison
                when (cond.op) {
                    ">"  -> varValNum > condValNum
                    ">=" -> varValNum >= condValNum
                    "<"  -> varValNum < condValNum
                    "<=" -> varValNum <= condValNum
                    "==" -> varValNum == condValNum
                    "!=" -> varValNum != condValNum
                    else -> false
                }
            } else {
                // String comparison
                val varStr = rawVar.toString()
                val condStr = cond.value.toString()
                when (cond.op) {
                    "==" -> varStr == condStr
                    "!=" -> varStr != condStr
                    else -> false
                }
            }
            if (matches) return cond.dest
        }
        return null
    }

    fun goBack() {
        val hist = _test.value.history
        if (hist.isEmpty()) return
        val last = hist.last()
        val prev = hist.dropLast(1)
        _test.value = _test.value.copy(
            history = prev,
            currentNodeId = last.nodeId,
            vars = last.vars.toMutableMap()
        )
    }

    fun showSaveBanner() {
        _test.value = _test.value.copy(showSaveBanner = true)
    }

    fun dismissSaveBanner() {
        _test.value = _test.value.copy(showSaveBanner = false)
    }

    fun confirmSaveRun() {
        viewModelScope.launch {
            val state = _test.value
            val steps = state.history.map { entry ->
                RunStep(
                    nodeId = entry.nodeId,
                    next = entry.nextId,
                    answer = entry.answer,
                    timestamp = System.currentTimeMillis()
                )
            }
            val fmt = SimpleDateFormat("yyMMdd-HHmm", Locale.US)
            val runId = "run-${fmt.format(Date())}"
            val varsSnapshot = state.vars.mapValues { it.value.toString() }
            repo.saveRun(RunEntity(runId, System.currentTimeMillis(), steps, varsSnapshot))

            // Reset test
            val firstNode = _parsedNodes.keys.firstOrNull() ?: ""
            _test.value = TestState(
                active = true,
                currentNodeId = firstNode,
                history = emptyList(),
                vars = mutableMapOf(),
                showSaveBanner = false
            )
        }
    }

    // ── Runs overlay ──────────────────────────────────────────────────────────

    /** Returns all runs (used by TreeStatsScreen which needs full history) */
    suspend fun getAllRunsDirect() = repo.getAllRuns()

    fun loadRuns(filter: String = _runs.value.filter) {
        viewModelScope.launch {
            val list = when (filter) {
                "7d"  -> repo.getRunsSince(7)
                "30d" -> repo.getRunsSince(30)
                else  -> repo.getAllRuns()
            }
            _runs.value = RunsState(runs = list, filter = filter)
        }
    }

    fun setRunsFilter(filter: String) { loadRuns(filter) }

    // ── Vars overlay ──────────────────────────────────────────────────────────

    fun loadVars() {
        viewModelScope.launch {
            val allRuns = repo.getAllRuns()
            val stats = repo.computeGlobalStats(allRuns)
            val overrides = repo.getVarOverrides()
            _vars.value = VarsState(stats = stats, overrides = overrides)
        }
    }

    fun setVarOverride(key: String, value: String) {
        viewModelScope.launch {
            repo.setVarOverride(key, value)
            loadVars()
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    fun getNode(id: String): TreeNode? = _parsedNodes[id]
    fun getParsedNodes(): Map<String, TreeNode> = _parsedNodes
    fun getParseErrors(): List<String> = _parseErrors
}
