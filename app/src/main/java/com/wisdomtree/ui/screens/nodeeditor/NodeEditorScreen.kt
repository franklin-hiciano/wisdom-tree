package com.wisdomtree.ui.screens.nodeeditor

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtree.parser.NodeType
import com.wisdomtree.parser.TreeNode
import com.wisdomtree.parser.TreeOption
import com.wisdomtree.ui.theme.WTColors

// ── Editable node model (mutable, detached from parser) ───────────────────────

data class EditableOption(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String = "",
    val next: String = ""       // empty = bare (multi), ">>" = default
)

data class EditableNode(
    val id: String,
    val title: String = "",
    val type: NodeType = NodeType.SINGLE,
    val options: List<EditableOption> = emptyList(),
    val defaultNext: String = "",
    val setStatements: List<String> = emptyList(),  // raw "@set key op val"
    val ifStatements: List<String> = emptyList()    // raw "@if key op val >> dest"
)

fun EditableNode.toSource(): String {
    val sb = StringBuilder()
    sb.appendLine(title.ifBlank { id })
    setStatements.forEach { sb.appendLine("  $it") }
    ifStatements.forEach  { sb.appendLine("  $it") }
    when (type) {
        NodeType.SINGLE -> options.forEach { o ->
            if (o.next.isNotBlank()) sb.appendLine("  ${o.label} >> ${o.next}")
            else sb.appendLine("  ${o.label}")
        }
        NodeType.MULTI -> {
            options.forEach { o -> sb.appendLine("  ${o.label}") }
            if (defaultNext.isNotBlank()) sb.appendLine("  >> $defaultNext")
        }
        NodeType.TEXT -> {
            if (defaultNext.isNotBlank()) sb.appendLine("  >> $defaultNext")
        }
        NodeType.NONE -> {}
    }
    return sb.toString().trimEnd()
}

fun TreeNode.toEditable(): EditableNode {
    val rawOpts = opts.map { o ->
        EditableOption(label = o.label, next = o.next ?: "")
    }
    val rawSets = sets.map { "@set ${it.key} ${it.op} ${it.value}" }
    val rawIfs  = conditions.map { c ->
        val win = if (c.windowDays != null) "${c.key}[${c.windowDays}d]" else c.key
        "@if $win ${c.op} ${c.value} >> ${c.dest}"
    }
    return EditableNode(
        id = id,
        title = id,
        type = type,
        options = rawOpts,
        defaultNext = def ?: "",
        setStatements = rawSets,
        ifStatements = rawIfs
    )
}

// ── Rebuild full source from a list of editable nodes ─────────────────────────
fun rebuildSource(nodes: List<EditableNode>): String =
    nodes.joinToString("\n\n") { it.toSource() }

// ── Node Editor Screen ────────────────────────────────────────────────────────

@Composable
fun NodeEditorScreen(
    nodes: Map<String, TreeNode>,
    onSourceChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Convert parsed nodes to editable list (preserving order)
    val editableNodes = remember(nodes) {
        nodes.values.map { it.toEditable() }.toMutableStateList()
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Any mutation flushes back to source
    fun flush() {
        onSourceChange(rebuildSource(editableNodes))
    }

    Row(modifier = modifier.fillMaxSize().background(WTColors.Bg)) {

        // ── Left panel: node list ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .background(WTColors.Surf)
                .border(BorderStroke(1.dp, WTColors.Border2))
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(WTColors.Surf2)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("nodes", color = WTColors.Muted2, fontSize = 10.sp,
                    fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
                    letterSpacing = 0.12.sp, modifier = Modifier.weight(1f))
                // Add node
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(WTColors.AccentDim)
                        .border(BorderStroke(1.dp, Color(0xFF2A3A10)), RoundedCornerShape(3.dp))
                        .clickable { showAddDialog = true }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) { Text("+", color = WTColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                editableNodes.forEachIndexed { i, node ->
                    val isSelected = selectedIndex == i
                    val typeColor = when (node.type) {
                        NodeType.SINGLE -> WTColors.Accent
                        NodeType.MULTI  -> WTColors.Accent3
                        NodeType.TEXT   -> WTColors.Accent5
                        NodeType.NONE   -> WTColors.Muted
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) WTColors.AccentDim else Color.Transparent)
                            .border(
                                BorderStroke(if (isSelected) 1.dp else 0.dp,
                                    if (isSelected) WTColors.Accent else Color.Transparent)
                            )
                            .clickable { selectedIndex = i }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(typeColor)
                        )
                        Text(
                            node.title.ifBlank { node.id },
                            color = if (isSelected) WTColors.Accent else WTColors.Text,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Delete
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .clickable {
                                    editableNodes.removeAt(i)
                                    if (selectedIndex == i) selectedIndex = null
                                    else if (selectedIndex != null && selectedIndex!! > i)
                                        selectedIndex = selectedIndex!! - 1
                                    flush()
                                }
                                .padding(3.dp)
                        ) {
                            Text("✕", color = WTColors.Muted, fontSize = 9.sp)
                        }
                    }
                    HorizontalDivider(color = WTColors.Border, thickness = 0.5.dp)
                }
            }

            // Node count footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WTColors.Surf2)
                    .padding(12.dp, 8.dp)
            ) {
                Text(
                    "${editableNodes.size} node${if (editableNodes.size != 1) "s" else ""}",
                    color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                )
            }
        }

        // ── Right panel: node detail editor ──────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(WTColors.Bg)
        ) {
            val idx = selectedIndex
            if (idx == null || idx >= editableNodes.size) {
                // Empty state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("select a node to edit", color = WTColors.Muted2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(6.dp))
                        Text("or press + to add one", color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                NodeDetailEditor(
                    node = editableNodes[idx],
                    allNodeIds = editableNodes.map { it.title.ifBlank { it.id } },
                    onUpdate = { updated ->
                        editableNodes[idx] = updated
                        flush()
                    }
                )
            }
        }
    }

    // ── Add node dialog ───────────────────────────────────────────────────────
    if (showAddDialog) {
        AddNodeDialog(
            existingIds = editableNodes.map { it.title.ifBlank { it.id } },
            onAdd = { newNode ->
                editableNodes.add(newNode)
                selectedIndex = editableNodes.size - 1
                showAddDialog = false
                flush()
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

// ── Node detail editor ────────────────────────────────────────────────────────

@Composable
fun NodeDetailEditor(
    node: EditableNode,
    allNodeIds: List<String>,
    onUpdate: (EditableNode) -> Unit
) {
    var title by remember(node.id) { mutableStateOf(node.title) }
    var type by remember(node.id) { mutableStateOf(node.type) }
    var options by remember(node.id) { mutableStateOf(node.options.toMutableList()) }
    var defaultNext by remember(node.id) { mutableStateOf(node.defaultNext) }
    var setStmts by remember(node.id) { mutableStateOf(node.setStatements.joinToString("\n")) }
    var ifStmts by remember(node.id) { mutableStateOf(node.ifStatements.joinToString("\n")) }

    fun push() {
        onUpdate(node.copy(
            title = title,
            type = type,
            options = options.toList(),
            defaultNext = defaultNext,
            setStatements = setStmts.lines().filter { it.isNotBlank() },
            ifStatements = ifStmts.lines().filter { it.isNotBlank() }
        ))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title
        EditorSection("node title") {
            WTTextField(value = title, onValueChange = { title = it; push() }, placeholder = "node title / id")
        }

        // Type picker
        EditorSection("node type") {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NodeType.values().forEach { t ->
                    val tColor = when (t) {
                        NodeType.SINGLE -> WTColors.Accent
                        NodeType.MULTI  -> WTColors.Accent3
                        NodeType.TEXT   -> WTColors.Accent5
                        NodeType.NONE   -> WTColors.Muted
                    }
                    val sel = type == t
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (sel) tColor.copy(alpha = 0.12f) else Color.Transparent)
                            .border(BorderStroke(1.dp, if (sel) tColor else WTColors.Border2), RoundedCornerShape(4.dp))
                            .clickable { type = t; push() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(t.name.lowercase(), color = if (sel) tColor else WTColors.Muted2,
                            fontSize = 11.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                when (type) {
                    NodeType.SINGLE -> "Each option routes to a different node"
                    NodeType.MULTI  -> "Multiple options selectable, all route to one destination"
                    NodeType.TEXT   -> "Free-text response, routes to one destination"
                    NodeType.NONE   -> "Info/pass-through node, no choices"
                },
                color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace
            )
        }

        // Options
        when (type) {
            NodeType.SINGLE -> EditorSection("options  (label >> destination)") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEachIndexed { i, opt ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            WTTextField(
                                value = opt.label, placeholder = "label",
                                modifier = Modifier.weight(1f),
                                onValueChange = { v -> options = options.toMutableList().also { it[i] = opt.copy(label = v) }; push() }
                            )
                            Text(">>", color = WTColors.Accent4, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            DestDropdown(
                                value = opt.next, allNodeIds = allNodeIds,
                                modifier = Modifier.weight(1f),
                                onSelect = { v -> options = options.toMutableList().also { it[i] = opt.copy(next = v) }; push() }
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .clickable { options = options.toMutableList().also { it.removeAt(i) }; push() }
                                    .padding(6.dp)
                            ) { Text("✕", color = WTColors.Muted2, fontSize = 10.sp) }
                        }
                    }
                    AddRowButton("add option") {
                        options = options.toMutableList().also { it.add(EditableOption()) }
                        push()
                    }
                }
            }
            NodeType.MULTI -> EditorSection("options  (bare labels, all route to default)") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEachIndexed { i, opt ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            WTTextField(
                                value = opt.label, placeholder = "option label",
                                modifier = Modifier.weight(1f),
                                onValueChange = { v -> options = options.toMutableList().also { it[i] = opt.copy(label = v) }; push() }
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .clickable { options = options.toMutableList().also { it.removeAt(i) }; push() }
                                    .padding(6.dp)
                            ) { Text("✕", color = WTColors.Muted2, fontSize = 10.sp) }
                        }
                    }
                    AddRowButton("add option") {
                        options = options.toMutableList().also { it.add(EditableOption()) }
                        push()
                    }
                }
            }
            else -> {}
        }

        // Default destination (multi / text / none)
        if (type != NodeType.SINGLE) {
            EditorSection("default destination  (>> next)") {
                DestDropdown(
                    value = defaultNext,
                    allNodeIds = allNodeIds,
                    modifier = Modifier.fillMaxWidth(),
                    onSelect = { defaultNext = it; push() }
                )
            }
        }

        // @set directives
        EditorSection("@set directives  (one per line: @set key op val)") {
            WTTextField(
                value = setStmts,
                onValueChange = { setStmts = it; push() },
                placeholder = "@set score += 1\n@set mood = happy",
                minLines = 2
            )
        }

        // @if conditions
        EditorSection("@if conditions  (one per line: @if key op val >> dest)") {
            WTTextField(
                value = ifStmts,
                onValueChange = { ifStmts = it; push() },
                placeholder = "@if score >= 3 >> celebrate\n@if mood == sad >> support",
                minLines = 2
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Shared sub-components ─────────────────────────────────────────────────────

@Composable
fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Default, letterSpacing = 0.12.sp)
        content()
    }
}

@Composable
fun WTTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier.fillMaxWidth(),
    minLines: Int = 1
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(WTColors.Surf)
            .border(BorderStroke(1.dp, WTColors.Border2), RoundedCornerShape(5.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = WTColors.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = WTColors.Text, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp),
            cursorBrush = SolidColor(WTColors.Accent),
            minLines = minLines,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DestDropdown(
    value: String,
    allNodeIds: List<String>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var text by remember(value) { mutableStateOf(value) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(5.dp))
                .background(WTColors.Surf)
                .border(BorderStroke(1.dp, WTColors.Border2), RoundedCornerShape(5.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 11.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text.ifBlank { "— none —" },
                color = if (text.isBlank()) WTColors.Muted else WTColors.Accent3,
                fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f)
            )
            Text("∨", color = WTColors.Muted, fontSize = 9.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(WTColors.Surf2).border(BorderStroke(1.dp, WTColors.Border2))
        ) {
            DropdownMenuItem(
                text = { Text("— none —", color = WTColors.Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                onClick = { text = ""; onSelect(""); expanded = false }
            )
            allNodeIds.forEach { id ->
                DropdownMenuItem(
                    text = { Text(id, color = WTColors.Accent3, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    onClick = { text = id; onSelect(id); expanded = false }
                )
            }
        }
    }
}

@Composable
fun AddRowButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(Color.Transparent)
            .border(BorderStroke(1.dp, WTColors.Border2), RoundedCornerShape(5.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("+ $label", color = WTColors.Muted2, fontSize = 11.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold)
    }
}

// ── Add node dialog ───────────────────────────────────────────────────────────

@Composable
fun AddNodeDialog(
    existingIds: List<String>,
    onAdd: (EditableNode) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(NodeType.SINGLE) }
    val error = when {
        title.isBlank() -> "title required"
        existingIds.contains(title.trim()) -> "a node with this name already exists"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WTColors.Surf,
        titleContentColor = WTColors.Text,
        title = { Text("new node", fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("title", color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Default)
                    WTTextField(value = title, onValueChange = { title = it }, placeholder = "What did you accomplish today?")
                    if (error != null) Text(error, color = WTColors.Accent2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("type", color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Default)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NodeType.values().forEach { t ->
                            val sel = type == t
                            val tColor = when (t) {
                                NodeType.SINGLE -> WTColors.Accent
                                NodeType.MULTI  -> WTColors.Accent3
                                NodeType.TEXT   -> WTColors.Accent5
                                NodeType.NONE   -> WTColors.Muted
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (sel) tColor.copy(alpha = 0.12f) else Color.Transparent)
                                    .border(BorderStroke(1.dp, if (sel) tColor else WTColors.Border2), RoundedCornerShape(4.dp))
                                    .clickable { type = t }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(t.name.lowercase(), color = if (sel) tColor else WTColors.Muted2,
                                    fontSize = 10.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (error == null) onAdd(EditableNode(id = title.trim(), title = title.trim(), type = type)) },
                enabled = error == null,
                colors = ButtonDefaults.buttonColors(containerColor = WTColors.Accent, contentColor = Color.Black)
            ) { Text("add node", fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("cancel", color = WTColors.Muted, fontSize = 11.sp) }
        }
    )
}
