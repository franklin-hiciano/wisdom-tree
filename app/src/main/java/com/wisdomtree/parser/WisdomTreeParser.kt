package com.wisdomtree.parser

// ── Data model ────────────────────────────────────────────────────────────────

data class TreeOption(val label: String, val next: String?)

data class SetAction(val key: String, val op: String, val value: Any)

data class Condition(
    val key: String,
    val windowDays: Int?,   // null = all-time
    val op: String,
    val value: Any,
    val dest: String
)

enum class NodeType { SINGLE, MULTI, TEXT, NONE }

data class TreeNode(
    val id: String,
    val title: String,
    val type: NodeType,
    val opts: List<TreeOption>,
    val def: String?,           // default next destination
    val sets: List<SetAction>,
    val conditions: List<Condition>,
    val inlineWin: Int?         // @inline [Nd] window in days; null = all-time
)

data class ParseResult(
    val nodes: LinkedHashMap<String, TreeNode>,
    val nodeLines: Map<String, LineRange>,
    val errors: List<String>
)

data class LineRange(val start: Int, val end: Int)

// ── Parser ────────────────────────────────────────────────────────────────────

object WisdomTreeParser {

    fun parse(src: String): ParseResult {
        val lines = src.split("\n")
        val nodes = LinkedHashMap<String, TreeNode>()
        val nodeLines = mutableMapOf<String, LineRange>()

        var curId: String? = null
        var curStart = 0
        val curOpts = mutableListOf<TreeOption>()
        var curDef: String? = null
        val curSets = mutableListOf<SetAction>()
        val curConds = mutableListOf<Condition>()
        var curInlineWin: Int? = -1  // -1 = not set yet

        fun flush() {
            val id = curId ?: return
            val routed = curOpts.any { it.next != null }
            val bare = curOpts.any { it.next == null }
            val type = when {
                routed && !bare -> NodeType.SINGLE
                bare && curDef != null -> NodeType.MULTI
                curOpts.isEmpty() && curDef != null -> NodeType.TEXT
                else -> NodeType.NONE
            }
            nodes[id] = TreeNode(
                id = id,
                title = id,
                type = type,
                opts = curOpts.toList(),
                def = curDef,
                sets = curSets.toList(),
                conditions = curConds.toList(),
                inlineWin = if (curInlineWin == -1) null else curInlineWin
            )
        }

        lines.forEachIndexed { i, rawLine ->
            val tr = rawLine.trim()
            if (tr.isEmpty() || tr.startsWith("#")) return@forEachIndexed

            val isIndented = rawLine.startsWith("  ") || rawLine.startsWith("\t")

            if (!isIndented) {
                // New node — flush previous
                if (curId != null) {
                    nodeLines[curId!!] = LineRange(curStart, i - 1)
                    flush()
                }
                curId = tr
                curStart = i
                curOpts.clear()
                curDef = null
                curSets.clear()
                curConds.clear()
                curInlineWin = -1
            } else {
                // Indented line — option / directive
                when {
                    tr.startsWith("@set ") -> {
                        val parts = tr.removePrefix("@set ").trim().split("\\s+".toRegex(), 3)
                        if (parts.size == 3) {
                            curSets.add(SetAction(parts[0], parts[1], parseVal(parts[2])))
                        }
                    }
                    tr.startsWith("@if ") -> {
                        // @if key[Nd] op val >> dest
                        val rest = tr.removePrefix("@if ").trim()
                        val arrowIdx = rest.indexOf(">>")
                        if (arrowIdx >= 0) {
                            val dest = rest.substring(arrowIdx + 2).trim()
                            val lhs = rest.substring(0, arrowIdx).trim()
                            // parse key[Nd] op val
                            val m = Regex("^(\\S+?)(?:\\[(\\d+)d])?\\s+(\\S+)\\s+(.+)$").find(lhs)
                            if (m != null) {
                                val (key, winStr, op, valStr) = m.destructured
                                curConds.add(
                                    Condition(
                                        key = key,
                                        windowDays = winStr.toIntOrNull(),
                                        op = op,
                                        value = parseVal(valStr),
                                        dest = dest
                                    )
                                )
                            }
                        }
                    }
                    tr.startsWith("@inline") -> {
                        val m = Regex("\\[(\\d+)d]").find(tr)
                        curInlineWin = m?.groupValues?.get(1)?.toIntOrNull()
                    }
                    tr.startsWith(">>") -> {
                        curDef = tr.removePrefix(">>").trim()
                    }
                    tr.contains(">>") -> {
                        val idx = tr.indexOf(">>")
                        val label = tr.substring(0, idx).trimEnd()
                        val next = tr.substring(idx + 2).trim()
                        curOpts.add(TreeOption(label = label, next = next.ifEmpty { null }))
                    }
                    else -> {
                        curOpts.add(TreeOption(label = tr, next = null))
                    }
                }
            }
        }

        // flush last node
        if (curId != null) {
            nodeLines[curId!!] = LineRange(curStart, lines.size - 1)
            flush()
        }

        val errors = validate(nodes)
        return ParseResult(nodes, nodeLines, errors)
    }

    private fun parseVal(s: String): Any {
        val t = s.trim()
        if (t.startsWith("\"") && t.endsWith("\"")) return t.drop(1).dropLast(1)
        return t.toDoubleOrNull() ?: t
    }

    private fun validate(nodes: Map<String, TreeNode>): List<String> {
        val errors = mutableListOf<String>()
        nodes.forEach { (k, n) ->
            n.opts.forEach { o ->
                if (o.next != null && !nodes.containsKey(o.next))
                    errors.add("\"$k\" → unknown \"${o.next}\"")
            }
            if (n.def != null && !nodes.containsKey(n.def))
                errors.add("\"$k\" default → unknown \"${n.def}\"")
            n.conditions.forEach { c ->
                if (!nodes.containsKey(c.dest))
                    errors.add("\"$k\" @if → unknown \"${c.dest}\"")
            }
        }
        return errors
    }

    // ── Syntax highlighting spans info ────────────────────────────────────────

    enum class SpanType { TITLE, ARROW, DEST, OPTION, COMMENT, SET, IF, INLINE }

    data class HighlightSpan(val start: Int, val end: Int, val type: SpanType)

    fun highlight(src: String): List<HighlightSpan> {
        val spans = mutableListOf<HighlightSpan>()
        var pos = 0
        src.split("\n").forEach { line ->
            val tr = line.trim()
            val lineEnd = pos + line.length
            if (tr.isNotEmpty() && !tr.startsWith("#")) {
                val isIndented = line.startsWith("  ") || line.startsWith("\t")
                val wsLen = line.length - line.trimStart().length
                when {
                    !isIndented -> spans.add(HighlightSpan(pos, lineEnd, SpanType.TITLE))
                    tr.startsWith("@set ") -> spans.add(HighlightSpan(pos + wsLen, lineEnd, SpanType.SET))
                    tr.startsWith("@if ") -> spans.add(HighlightSpan(pos + wsLen, lineEnd, SpanType.IF))
                    tr.startsWith("@inline") -> spans.add(HighlightSpan(pos + wsLen, lineEnd, SpanType.INLINE))
                    tr.startsWith(">>") -> {
                        spans.add(HighlightSpan(pos + wsLen, pos + wsLen + 2, SpanType.ARROW))
                        spans.add(HighlightSpan(pos + wsLen + 2, lineEnd, SpanType.DEST))
                    }
                    tr.contains(">>") -> {
                        val idx = tr.indexOf(">>")
                        spans.add(HighlightSpan(pos + wsLen, pos + wsLen + idx, SpanType.OPTION))
                        spans.add(HighlightSpan(pos + wsLen + idx, pos + wsLen + idx + 2, SpanType.ARROW))
                        spans.add(HighlightSpan(pos + wsLen + idx + 2, lineEnd, SpanType.DEST))
                    }
                    else -> spans.add(HighlightSpan(pos + wsLen, lineEnd, SpanType.OPTION))
                }
            } else if (tr.startsWith("#")) {
                spans.add(HighlightSpan(pos, lineEnd, SpanType.COMMENT))
            }
            pos = lineEnd + 1 // +1 for the \n
        }
        return spans
    }
}
