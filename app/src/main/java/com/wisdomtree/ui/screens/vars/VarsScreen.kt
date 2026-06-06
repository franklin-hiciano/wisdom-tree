package com.wisdomtree.ui.screens.vars

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtree.data.repository.NodeStat
import com.wisdomtree.ui.theme.WTColors
import com.wisdomtree.viewmodel.VarsState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VarsScreen(
    state: VarsState,
    onSetOverride: (String, String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WTColors.Bg)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("variable inspector", color = WTColors.Accent5, fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
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

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            val stats = state.stats

            // ── Global vars ───────────────────────────────────────────────────
            VarsSection(title = "global variables") {
                if (stats == null || stats.vars.isEmpty()) {
                    Text("no variables set yet — use @set in your tree",
                        color = WTColors.Muted, fontSize = 12.sp, fontStyle = FontStyle.Italic, fontFamily = FontFamily.Monospace)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        stats.vars.forEach { (k, v) ->
                            VarRow(key = k, value = v, onSet = { newVal -> onSetOverride(k, newVal) })
                        }
                    }
                }
            }

            // ── Node stats ────────────────────────────────────────────────────
            VarsSection(title = "node stats · all time") {
                if (stats == null || stats.nodeStats.isEmpty()) {
                    Text("no runs recorded yet",
                        color = WTColors.Muted, fontSize = 12.sp, fontStyle = FontStyle.Italic, fontFamily = FontFamily.Monospace)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        stats.nodeStats.forEach { (nodeId, stat) ->
                            if (stat.options.isNotEmpty() || stat.texts.isNotEmpty()) {
                                NodeStatRow(nodeId = nodeId, stat = stat)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VarsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Default, letterSpacing = 0.14.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun VarRow(key: String, value: String, onSet: (String) -> Unit) {
    var inputVal by remember(key, value) { mutableStateOf(value) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(WTColors.Surf)
            .border(BorderStroke(1.dp, WTColors.Border), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(key, color = WTColors.Accent5, fontSize = 11.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

        // Value input
        Box(
            modifier = Modifier
                .width(140.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(WTColors.Surf2)
                .border(BorderStroke(1.dp, WTColors.Border2), RoundedCornerShape(4.dp))
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            BasicTextField(
                value = inputVal,
                onValueChange = { inputVal = it },
                textStyle = TextStyle(color = WTColors.Text, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(WTColors.Accent5),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Set button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF12082A))
                .border(BorderStroke(1.dp, Color(0xFF3A1A60)), RoundedCornerShape(4.dp))
                .clickable { onSet(inputVal) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("set", color = WTColors.Accent5, fontSize = 9.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, letterSpacing = 0.06.sp)
        }
    }
}

@Composable
private fun NodeStatRow(nodeId: String, stat: NodeStat) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WTColors.Surf),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, WTColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(nodeId, color = WTColors.Accent, fontSize = 11.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold)

            if (stat.options.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    stat.options.entries.sortedByDescending { it.value }.forEach { (label, count) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(WTColors.Surf2)
                                .border(BorderStroke(1.dp, WTColors.Border2), RoundedCornerShape(2.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("$label ($count)", color = WTColors.Muted3, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            if (stat.texts.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                val dateFmt = SimpleDateFormat("MMM d", Locale.US)
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    stat.texts.takeLast(5).reversed().forEachIndexed { i, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .then(if (i > 0) Modifier.border(BorderStroke(0.dp, Color.Transparent)) else Modifier),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(dateFmt.format(Date(entry.timestamp)), color = WTColors.Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                if (entry.text.length > 120) entry.text.take(120) + "…" else entry.text,
                                color = WTColors.Muted3, fontSize = 10.sp, fontFamily = FontFamily.Monospace, lineHeight = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (i < stat.texts.size.coerceAtMost(5) - 1) HorizontalDivider(color = WTColors.Border, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
