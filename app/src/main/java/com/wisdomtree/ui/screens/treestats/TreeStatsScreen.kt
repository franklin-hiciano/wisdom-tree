package com.wisdomtree.ui.screens.treestats

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtree.data.model.RunEntity
import com.wisdomtree.data.model.RunStep
import com.wisdomtree.parser.TreeNode
import com.wisdomtree.ui.theme.WTColors
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

// ── Tree stats screen ─────────────────────────────────────────────────────────

@Composable
fun TreeStatsScreen(
    runs: List<RunEntity>,
    nodes: Map<String, TreeNode>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }   // 0=overview, 1=runs, 2=nodes
    val tabs = listOf("overview", "runs", "nodes")

    Column(modifier = modifier.fillMaxSize().background(WTColors.Bg)) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("tree stats", color = WTColors.Accent4,
                fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)

            // Tabs
            tabs.forEachIndexed { i, label ->
                val sel = selectedTab == i
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (sel) Color(0xFF1A1000) else Color.Transparent)
                        .border(BorderStroke(1.dp, if (sel) WTColors.Accent4 else WTColors.Border2), RoundedCornerShape(3.dp))
                        .clickable { selectedTab = i }
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(label, color = if (sel) WTColors.Accent4 else WTColors.Muted2,
                        fontSize = 10.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = onClose,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WTColors.Muted2),
                border = BorderStroke(1.dp, WTColors.Border2),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) { Text("esc", fontSize = 10.sp, fontFamily = FontFamily.Default) }
        }

        HorizontalDivider(color = WTColors.Border2)

        when (selectedTab) {
            0 -> OverviewTab(runs = runs, nodes = nodes)
            1 -> RunsTab(runs = runs)
            2 -> NodesTab(runs = runs, nodes = nodes)
        }
    }
}

// ── Overview tab ──────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(runs: List<RunEntity>, nodes: Map<String, TreeNode>) {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }.timeInMillis
    val week = today - 7 * 24 * 3600 * 1000L
    val month = today - 30 * 24 * 3600 * 1000L

    val totalRuns = runs.size
    val runsThisWeek = runs.count { it.savedAt >= week }
    val runsThisMonth = runs.count { it.savedAt >= month }
    val avgSteps = if (runs.isEmpty()) 0.0 else runs.map { it.steps.size }.average()
    val longestStreak = computeStreak(runs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // KPI cards
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard("total runs", totalRuns.toString(), WTColors.Accent, modifier = Modifier.weight(1f))
            KpiCard("this week", runsThisWeek.toString(), WTColors.Accent3, modifier = Modifier.weight(1f))
            KpiCard("this month", runsThisMonth.toString(), WTColors.Accent5, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard("avg steps", "%.1f".format(avgSteps), WTColors.Accent4, modifier = Modifier.weight(1f))
            KpiCard("best streak", "${longestStreak}d", WTColors.Accent2, modifier = Modifier.weight(1f))
            KpiCard("nodes", nodes.size.toString(), WTColors.Muted2, modifier = Modifier.weight(1f))
        }

        // 30-day activity heatmap
        if (runs.isNotEmpty()) {
            StatsSection("last 30 days") {
                ActivityHeatmap(runs = runs)
            }
        }

        // Most common paths
        if (runs.isNotEmpty()) {
            StatsSection("most common entry nodes") {
                val entryCounts = runs.groupBy { it.steps.firstOrNull()?.nodeId ?: "?" }
                    .mapValues { it.value.size }
                    .entries.sortedByDescending { it.value }.take(5)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    entryCounts.forEach { (nodeId, count) ->
                        BarRow(label = nodeId, value = count, max = entryCounts.first().value, color = WTColors.Accent)
                    }
                }
            }
        }

        // Variables over time
        val allVarKeys = runs.flatMap { it.vars.keys }.toSet()
        if (allVarKeys.isNotEmpty()) {
            StatsSection("variable history") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    allVarKeys.forEach { key ->
                        val vals = runs.sortedBy { it.savedAt }
                            .mapNotNull { r -> r.vars[key]?.toDoubleOrNull()?.let { r.savedAt to it } }
                        if (vals.isNotEmpty()) {
                            VarSparkline(key = key, data = vals)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(WTColors.Surf)
            .border(BorderStroke(1.dp, WTColors.Border2), RoundedCornerShape(8.dp))
            .padding(14.dp, 12.dp)
    ) {
        Text(value, color = color, fontSize = 22.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = WTColors.Muted2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ActivityHeatmap(runs: List<RunEntity>) {
    val now = System.currentTimeMillis()
    val dayMs = 24 * 3600 * 1000L
    // Build map: dayOffset -> count (0 = today, 29 = 30 days ago)
    val counts = mutableMapOf<Int, Int>()
    runs.forEach { r ->
        val daysAgo = ((now - r.savedAt) / dayMs).toInt()
        if (daysAgo in 0..29) counts[daysAgo] = (counts[daysAgo] ?: 0) + 1
    }
    val maxCount = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        // Render newest on right, oldest on left
        (29 downTo 0).forEach { daysAgo ->
            val count = counts[daysAgo] ?: 0
            val intensity = count.toFloat() / maxCount
            val bg = if (count == 0) WTColors.Surf2
                     else WTColors.Accent.copy(alpha = 0.2f + intensity * 0.8f)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(bg)
                    .border(BorderStroke(1.dp, WTColors.Border), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (count > 0) Text(
                    count.toString(), color = Color.Black, fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("30 days ago", color = WTColors.Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text("today", color = WTColors.Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun BarRow(label: String, value: Int, max: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label, color = WTColors.Text, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.width(120.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(WTColors.Surf2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(value.toFloat() / max.toFloat())
                    .background(color)
            )
        }
        Text(value.toString(), color = WTColors.Muted2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun VarSparkline(key: String, data: List<Pair<Long, Double>>) {
    val minVal = data.minOf { it.second }
    val maxVal = data.maxOf { it.second }.let { if (it == minVal) minVal + 1 else it }
    val latest = data.last().second

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(key, color = WTColors.Accent5, fontSize = 11.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold)
            Text(
                if (latest == latest.toLong().toDouble()) latest.toLong().toString() else "%.2f".format(latest),
                color = WTColors.Text, fontSize = 11.sp, fontFamily = FontFamily.Monospace
            )
        }
        // Sparkline
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { (_, v) ->
                val frac = ((v - minVal) / (maxVal - minVal)).toFloat().coerceIn(0.05f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(frac)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(WTColors.Accent5.copy(alpha = 0.6f))
                )
            }
        }
    }
}

// ── Runs tab ──────────────────────────────────────────────────────────────────

@Composable
private fun RunsTab(runs: List<RunEntity>) {
    val dateFmt = SimpleDateFormat("MMM d, h:mm a", Locale.US)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (runs.isEmpty()) {
            Text("no runs yet — run your tree to see history",
                color = WTColors.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        runs.forEach { run ->
            var expanded by remember(run.runId) { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = WTColors.Surf),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, WTColors.Border2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dateFmt.format(Date(run.savedAt)),
                                color = WTColors.Text, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("${run.steps.size} steps · ${run.vars.size} var${if (run.vars.size != 1) "s" else ""}",
                                color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(if (expanded) "∨" else "›", color = WTColors.Muted, fontSize = 11.sp)
                    }
                    if (expanded) {
                        HorizontalDivider(color = WTColors.Border)
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            run.steps.forEachIndexed { i, step ->
                                RunStepRow(step = step, isLast = i == run.steps.size - 1)
                            }
                            if (run.vars.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    run.vars.forEach { (k, v) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(0xFF12082A))
                                                .border(BorderStroke(1.dp, Color(0xFF3A1A60)), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) { Text("$k=$v", color = WTColors.Accent5, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunStepRow(step: RunStep, isLast: Boolean) {
    val ansText = when (step.answer?.type) {
        "single" -> step.answer.label ?: ""
        "multi"  -> step.answer.selected?.joinToString(", ") ?: ""
        "text"   -> step.answer.text?.take(80) ?: ""
        else -> ""
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(step.nodeId, color = WTColors.Accent, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(130.dp), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text("→", color = WTColors.Muted, fontSize = 10.sp)
            Text(step.next ?: "—", color = WTColors.Accent3, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(100.dp), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(ansText, color = WTColors.Muted3, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f), maxLines = 2)
        }
        if (!isLast) HorizontalDivider(color = WTColors.Border, thickness = 0.5.dp)
    }
}

// ── Nodes tab ─────────────────────────────────────────────────────────────────

@Composable
private fun NodesTab(runs: List<RunEntity>, nodes: Map<String, TreeNode>) {
    // Compute per-node stats across all runs
    data class NStat(val visits: Int, val options: Map<String, Int>, val texts: List<String>)
    val nodeStats = mutableMapOf<String, NStat>()
    runs.forEach { run ->
        run.steps.forEach { step ->
            val cur = nodeStats[step.nodeId] ?: NStat(0, emptyMap(), emptyList())
            val newOpts = cur.options.toMutableMap()
            val newTexts = cur.texts.toMutableList()
            when (step.answer?.type) {
                "single" -> step.answer.label?.let { newOpts[it] = (newOpts[it] ?: 0) + 1 }
                "multi"  -> step.answer.selected?.forEach { newOpts[it] = (newOpts[it] ?: 0) + 1 }
                "text"   -> step.answer.text?.let { newTexts.add(it) }
            }
            nodeStats[step.nodeId] = NStat(cur.visits + 1, newOpts, newTexts)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (nodeStats.isEmpty()) {
            Text("no data yet — complete some runs first",
                color = WTColors.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        // Show all nodes, with stats where available
        nodes.keys.forEach { nodeId ->
            val stat = nodeStats[nodeId]
            val totalVisits = stat?.visits ?: 0
            val node = nodes[nodeId]!!

            Card(
                colors = CardDefaults.cardColors(containerColor = WTColors.Surf),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, WTColors.Border2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(nodeId, color = WTColors.Accent, fontSize = 12.sp,
                            fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(node.type.name.lowercase(), color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("$totalVisits visit${if (totalVisits != 1) "s" else ""}",
                                color = WTColors.Accent4, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Option breakdown
                    if (stat != null && stat.options.isNotEmpty()) {
                        val maxOpts = stat.options.values.maxOrNull() ?: 1
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            stat.options.entries.sortedByDescending { it.value }.forEach { (label, count) ->
                                BarRow(label = label, value = count, max = maxOpts, color = WTColors.Accent3)
                            }
                        }
                    }

                    // Text responses
                    if (stat != null && stat.texts.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            stat.texts.takeLast(3).reversed().forEach { t ->
                                Text(
                                    "\"${t.take(100)}${if (t.length > 100) "…" else ""}\"",
                                    color = WTColors.Muted3, fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace, lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // No data
                    if (totalVisits == 0) {
                        Text("not visited yet", color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun StatsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Default, letterSpacing = 0.14.sp)
        content()
    }
}

private fun computeStreak(runs: List<RunEntity>): Int {
    if (runs.isEmpty()) return 0
    val dayMs = 24 * 3600 * 1000L
    val days = runs.map { (it.savedAt / dayMs).toInt() }.toSortedSet().toList().reversed()
    var streak = 1
    var longest = 1
    for (i in 1 until days.size) {
        if (days[i - 1] - days[i] == 1) { streak++; longest = max(longest, streak) }
        else streak = 1
    }
    return longest
}
