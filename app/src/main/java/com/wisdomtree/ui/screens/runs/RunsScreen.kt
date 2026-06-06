package com.wisdomtree.ui.screens.runs

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtree.data.model.RunEntity
import com.wisdomtree.ui.theme.WTColors
import com.wisdomtree.viewmodel.RunsState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RunsScreen(
    state: RunsState,
    onFilterChange: (String) -> Unit,
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
            Text("runs", color = WTColors.Accent3, fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)

            // Filter chips
            Text("filter:", color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            listOf("7d", "30d", "all").forEach { f ->
                FilterChip(
                    selected = state.filter == f,
                    onClick = { onFilterChange(f) },
                    label = { Text(f, fontSize = 10.sp, fontFamily = FontFamily.Default) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Transparent,
                        selectedLabelColor = WTColors.Accent3,
                        containerColor = Color.Transparent,
                        labelColor = WTColors.Muted2
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = state.filter == f,
                        selectedBorderColor = WTColors.Accent3,
                        borderColor = WTColors.Border2
                    ),
                    modifier = Modifier.height(26.dp)
                )
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

        // ── Run list ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.runs.isEmpty()) {
                Text("no runs in this window", color = WTColors.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            } else {
                state.runs.forEach { run ->
                    RunCard(run = run)
                }
            }
        }
    }
}

@Composable
private fun RunCard(run: RunEntity) {
    var expanded by remember { mutableStateOf(false) }
    val dateFmt = SimpleDateFormat("MMM d, h:mm a", Locale.US)
    val date = dateFmt.format(Date(run.savedAt))

    Card(
        colors = CardDefaults.cardColors(containerColor = WTColors.Surf),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, WTColors.Border2),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(run.runId, color = WTColors.Accent3, fontSize = 10.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold)
                Text(date, color = WTColors.Muted2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.weight(1f))
                Text("${run.steps.size} step${if (run.steps.size != 1) "s" else ""}", color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(if (expanded) "∨" else "›", color = WTColors.Muted, fontSize = 10.sp)
            }

            // Expanded detail
            if (expanded) {
                HorizontalDivider(color = WTColors.Border)
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    run.steps.forEachIndexed { i, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .then(if (i < run.steps.size - 1) Modifier.border(
                                    BorderStroke(0.dp, Color.Transparent)) else Modifier),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                step.nodeId,
                                color = WTColors.Accent,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(140.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("→", color = WTColors.Muted, fontSize = 10.sp)
                            Text(
                                step.next ?: "—",
                                color = WTColors.Accent3,
                                fontSize = 10.sp,
                                modifier = Modifier.width(100.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Answer
                            val ansText = when (step.answer?.type) {
                                "single" -> step.answer.label ?: ""
                                "multi"  -> step.answer.selected?.joinToString(", ") ?: ""
                                "text"   -> step.answer.text?.take(60) ?: ""
                                else     -> ""
                            }
                            Text(
                                ansText,
                                color = WTColors.Muted3,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                                maxLines = 2
                            )
                        }
                        if (i < run.steps.size - 1) {
                            HorizontalDivider(color = WTColors.Border, thickness = 0.5.dp)
                        }
                    }

                    // Vars snapshot
                    if (run.vars.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = WTColors.Border)
                        Spacer(Modifier.height(10.dp))
                        Text("vars at save", color = WTColors.Muted, fontSize = 9.sp, fontFamily = FontFamily.Default, letterSpacing = 0.12.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            run.vars.forEach { (k, v) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(WTColors.Surf2)
                                        .border(BorderStroke(1.dp, Color(0xFF2A1A40)), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("$k = $v", color = WTColors.Accent5, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
