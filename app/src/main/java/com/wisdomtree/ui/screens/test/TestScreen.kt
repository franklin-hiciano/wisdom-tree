package com.wisdomtree.ui.screens.test

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtree.data.model.RunAnswer
import com.wisdomtree.parser.NodeType
import com.wisdomtree.parser.TreeNode
import com.wisdomtree.ui.theme.WTColors
import com.wisdomtree.viewmodel.TestState

@Composable
fun TestScreen(
    state: TestState,
    currentNode: TreeNode?,
    onBack: () -> Unit,
    onExit: () -> Unit,
    onAnswerSingle: (TreeNode, Int) -> Unit,
    onAnswerMulti: (TreeNode, List<String>) -> Unit,
    onAnswerText: (TreeNode, String) -> Unit,
    onSaveRun: () -> Unit,
    onConfirmSave: () -> Unit,
    onDismissSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WTColors.Bg)
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(WTColors.Bg)
                    .border(BorderStroke(1.dp, WTColors.Border2), shape = RoundedCornerShape(0.dp))
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "▶ test mode",
                    color = WTColors.Accent,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
                // Breadcrumb
                Text(
                    state.history.joinToString(" → ") { it.nodeId }.let {
                        if (it.length > 50) "…" + it.takeLast(47) else it
                    },
                    color = WTColors.Muted2,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                // Save button
                OutlinedButton(
                    onClick = onSaveRun,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WTColors.Accent),
                    border = BorderStroke(1.dp, WTColors.Accent),
                    contentPadding = PaddingValues(horizontal = 13.dp, vertical = 6.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("save & new run", fontSize = 10.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold)
                }
                // Esc button
                OutlinedButton(
                    onClick = onExit,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WTColors.Muted2),
                    border = BorderStroke(1.dp, WTColors.Border2),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("esc", fontSize = 10.sp, fontFamily = FontFamily.Default)
                }
            }

            HorizontalDivider(color = WTColors.Border2)

            // ── Cards ─────────────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                currentNode?.let { node ->
                    NodeCard(
                        node = node,
                        historySize = state.history.size,
                        onBack = onBack,
                        onAnswerSingle = { idx -> onAnswerSingle(node, idx) },
                        onAnswerMulti = { sel -> onAnswerMulti(node, sel) },
                        onAnswerText = { txt -> onAnswerText(node, txt) }
                    )
                } ?: run {
                    // End of tree
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        colors = CardDefaults.cardColors(containerColor = WTColors.Surf),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, WTColors.Border2)
                    ) {
                        Column(modifier = Modifier.padding(26.dp)) {
                            Text("end of tree", color = WTColors.Muted2, fontSize = 10.sp, fontFamily = FontFamily.Default, letterSpacing = 0.14.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("You've reached the end of this branch.", color = WTColors.Text, fontSize = 16.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (state.history.isNotEmpty()) {
                                    TextButton(onClick = onBack) {
                                        Text("← back", color = WTColors.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                Button(
                                    onClick = onSaveRun,
                                    colors = ButtonDefaults.buttonColors(containerColor = WTColors.Accent, contentColor = Color.Black)
                                ) {
                                    Text("save & new run", fontSize = 11.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Save banner ───────────────────────────────────────────────────────
        if (state.showSaveBanner) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = WTColors.Surf),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, WTColors.Border2)
                ) {
                    Column(modifier = Modifier.padding(28.dp, 28.dp, 28.dp, 22.dp)) {
                        Text("Save current run?", color = WTColors.Text, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "All steps will be recorded. The path is traced through the next pointers from wherever you went forward.",
                            color = WTColors.Muted2,
                            fontSize = 11.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = onDismissSave,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WTColors.Muted2),
                                border = BorderStroke(1.dp, WTColors.Border2)
                            ) { Text("cancel", fontSize = 10.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, letterSpacing = 0.06.sp) }
                            Button(
                                onClick = onConfirmSave,
                                colors = ButtonDefaults.buttonColors(containerColor = WTColors.Accent, contentColor = Color.Black)
                            ) { Text("save & start new", fontSize = 10.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, letterSpacing = 0.06.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeCard(
    node: TreeNode,
    historySize: Int,
    onBack: () -> Unit,
    onAnswerSingle: (Int) -> Unit,
    onAnswerMulti: (List<String>) -> Unit,
    onAnswerText: (String) -> Unit
) {
    var selectedIndices by remember(node.id) { mutableStateOf(setOf<Int>()) }
    var textInput by remember(node.id) { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = WTColors.Surf),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, WTColors.Border2)
    ) {
        Column(modifier = Modifier.padding(26.dp, 26.dp, 26.dp, 22.dp)) {

            // Node type eyebrow
            val eyebrowColor = when (node.type) {
                NodeType.SINGLE -> WTColors.Accent
                NodeType.MULTI  -> WTColors.Accent3
                NodeType.TEXT   -> WTColors.Accent5
                NodeType.NONE   -> WTColors.Muted
            }
            val eyebrowLabel = when (node.type) {
                NodeType.SINGLE -> "single choice"
                NodeType.MULTI  -> "multi select"
                NodeType.TEXT   -> "text response"
                NodeType.NONE   -> "info"
            }
            Text(eyebrowLabel, color = eyebrowColor, fontSize = 10.sp, fontFamily = FontFamily.Default, letterSpacing = 0.14.sp)
            Spacer(Modifier.height(8.dp))

            // Title
            Text(node.title, color = WTColors.Text, fontSize = 18.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
            Spacer(Modifier.height(18.dp))

            // Choices
            when (node.type) {
                NodeType.SINGLE -> {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        node.opts.forEachIndexed { idx, opt ->
                            ChoiceRow(
                                label = opt.label,
                                isMulti = false,
                                selected = false,
                                onClick = { onAnswerSingle(idx) }
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
                NodeType.MULTI -> {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        node.opts.forEachIndexed { idx, opt ->
                            ChoiceRow(
                                label = opt.label,
                                isMulti = true,
                                selected = idx in selectedIndices,
                                onClick = {
                                    selectedIndices = if (idx in selectedIndices)
                                        selectedIndices - idx else selectedIndices + idx
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
                NodeType.TEXT -> {
                    BasicOutlinedField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = "type your response…"
                    )
                    Spacer(Modifier.height(18.dp))
                }
                NodeType.NONE -> {
                    Spacer(Modifier.height(4.dp))
                }
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (historySize > 0) {
                    TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                        Text("← back", color = WTColors.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(Modifier.weight(1f))
                when (node.type) {
                    NodeType.MULTI -> {
                        Button(
                            onClick = {
                                val sel = selectedIndices.map { node.opts[it].label }
                                onAnswerMulti(sel)
                            },
                            enabled = selectedIndices.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WTColors.Accent,
                                contentColor = Color.Black,
                                disabledContainerColor = Color(0xFF1E1E1E),
                                disabledContentColor = WTColors.Muted
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) { Text("next →", fontSize = 11.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, letterSpacing = 0.06.sp) }
                    }
                    NodeType.TEXT -> {
                        Button(
                            onClick = { onAnswerText(textInput) },
                            enabled = textInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WTColors.Accent,
                                contentColor = Color.Black,
                                disabledContainerColor = Color(0xFF1E1E1E),
                                disabledContentColor = WTColors.Muted
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) { Text("next →", fontSize = 11.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, letterSpacing = 0.06.sp) }
                    }
                    NodeType.NONE -> {
                        if (node.def != null) {
                            Button(
                                onClick = { onAnswerSingle(0) },
                                colors = ButtonDefaults.buttonColors(containerColor = WTColors.Accent, contentColor = Color.Black),
                                shape = RoundedCornerShape(4.dp)
                            ) { Text("continue →", fontSize = 11.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold) }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, isMulti: Boolean, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) (if (isMulti) WTColors.Accent3 else WTColors.Accent) else WTColors.Border
    val bgColor = if (selected) (if (isMulti) WTColors.Accent3Dim else WTColors.AccentDim) else WTColors.Surf2

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isMulti) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (selected) WTColors.Accent3 else Color.Transparent)
                    .border(BorderStroke(1.5.dp, if (selected) WTColors.Accent3 else WTColors.Border3), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Text("✓", fontSize = 9.sp, color = Color.Black)
            }
        } else {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (selected) WTColors.Accent else Color.Transparent)
                    .border(BorderStroke(1.5.dp, if (selected) WTColors.Accent else WTColors.Border3), androidx.compose.foundation.shape.CircleShape)
            )
        }
        Text(label, color = WTColors.Text, fontSize = 12.sp, lineHeight = 17.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BasicOutlinedField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(WTColors.Surf2)
            .border(BorderStroke(1.dp, WTColors.Border2), RoundedCornerShape(6.dp))
            .padding(13.dp, 11.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = WTColors.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontStyle = FontStyle.Italic)
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = WTColors.Text,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 20.sp
            ),
            cursorBrush = SolidColor(WTColors.Accent),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 80.dp)
        )
    }
}

private fun SolidColor(color: androidx.compose.ui.graphics.Color) =
    androidx.compose.ui.graphics.SolidColor(color)
