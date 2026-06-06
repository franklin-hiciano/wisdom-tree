package com.wisdomtree.ui.screens.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.wisdomtree.parser.NodeType
import com.wisdomtree.parser.TreeNode
import com.wisdomtree.ui.theme.WTColors
import kotlin.math.sqrt

// ── Layout constants (matches HTML: NW=170 NH=48 CGAP=240 RGAP=72) ───────────
private const val NW = 170f
private const val NH = 48f
private const val CGAP = 240f
private const val RGAP = 72f

data class NodeRect(val x: Float, val y: Float, val w: Float = NW, val h: Float = NH)

fun computeLayout(nodes: Map<String, TreeNode>): Map<String, NodeRect> {
    val keys = nodes.keys.toList()
    if (keys.isEmpty()) return emptyMap()

    val depth = mutableMapOf(keys[0] to 0)
    val queue = ArrayDeque<String>().apply { add(keys[0]) }
    val visited = mutableSetOf<String>()

    while (queue.isNotEmpty()) {
        val id = queue.removeFirst()
        if (id in visited) continue
        visited.add(id)
        val node = nodes[id] ?: continue
        val push = { next: String ->
            if (next !in depth) {
                depth[next] = depth[id]!! + 1
                queue.add(next)
            }
        }
        node.opts.forEach { o -> o.next?.let { push(it) } }
        node.def?.let { push(it) }
    }

    var maxDepth = depth.values.maxOrNull() ?: 0
    keys.forEach { k -> if (k !in depth) depth[k] = ++maxDepth }

    val cols = mutableMapOf<Int, MutableList<String>>()
    keys.forEach { k -> cols.getOrPut(depth[k]!!) { mutableListOf() }.add(k) }

    val layout = mutableMapOf<String, NodeRect>()
    cols.forEach { (d, ns) ->
        val total = ns.size * NH + (ns.size - 1) * (RGAP - NH)
        ns.forEachIndexed { i, id ->
            layout[id] = NodeRect(
                x = d * CGAP,
                y = -total / 2 + i * RGAP
            )
        }
    }
    return layout
}

// ── Composable ────────────────────────────────────────────────────────────────

@Composable
fun TreeCanvas(
    nodes: Map<String, TreeNode>,
    selectedNodeId: String?,
    onNodeTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val layout = remember(nodes) { computeLayout(nodes) }

    var tx by remember { mutableFloatStateOf(60f) }
    var ty by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    val textMeasurer = rememberTextMeasurer()

    // Color constants for drawing
    val surfColor   = WTColors.Surf
    val borderColor = WTColors.Border2
    val textColor   = WTColors.Text
    val mutedColor  = WTColors.Muted
    val muted2Color = WTColors.Muted2
    val accentColor = WTColors.Accent
    val accent3Color= WTColors.Accent3
    val accent5Color= WTColors.Accent5

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Pan
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        tx += dragAmount.x
                        ty += dragAmount.y
                    }
                }
                .pointerInput(Unit) {
                    // Pinch zoom
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.2f, 3f)
                        tx += pan.x
                        ty += pan.y
                    }
                }
                .pointerInput(layout) {
                    // Tap to select
                    detectTapGestures { tapOffset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        layout.forEach { (id, rect) ->
                            val sx = cx + (rect.x + tx) * scale
                            val sy = cy + (rect.y + ty) * scale
                            val sw = rect.w * scale
                            val sh = rect.h * scale
                            if (tapOffset.x in sx..(sx + sw) && tapOffset.y in sy..(sy + sh)) {
                                onNodeTap(id)
                                return@detectTapGestures
                            }
                        }
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Draw edges first
            nodes.forEach { (id, node) ->
                val s = layout[id] ?: return@forEach
                val sx = cx + (s.x + NW + tx) * scale
                val sy = cy + (s.y + NH / 2 + ty) * scale

                fun drawEdge(destId: String, selected: Boolean) {
                    val d = layout[destId] ?: return
                    val ex = cx + (d.x + tx) * scale
                    val ey = cy + (d.y + NH / 2 + ty) * scale

                    val edgeColor = if (selected) accentColor else Color(0xFF2A2A2A)
                    val strokeW = if (selected) 1.5f * scale else 1f * scale

                    // Cubic bezier approximation via quadratic with control points
                    val cp = 70f * scale
                    val path = Path().apply {
                        moveTo(sx, sy)
                        cubicTo(sx + cp, sy, ex - cp, ey, ex, ey)
                    }
                    drawPath(path, color = edgeColor, style = Stroke(width = strokeW))

                    // Arrowhead
                    val arrowColor = if (selected) accentColor else Color(0xFF333333)
                    val arrowSize = 6f * scale
                    val angle = kotlin.math.atan2(ey - sy, ex - sx)
                    val ax1 = ex - arrowSize * kotlin.math.cos(angle - 0.5f)
                    val ay1 = ey - arrowSize * kotlin.math.sin(angle - 0.5f)
                    val ax2 = ex - arrowSize * kotlin.math.cos(angle + 0.5f)
                    val ay2 = ey - arrowSize * kotlin.math.sin(angle + 0.5f)
                    val arrowPath = Path().apply {
                        moveTo(ex, ey)
                        lineTo(ax1, ay1)
                        lineTo(ax2, ay2)
                        close()
                    }
                    drawPath(arrowPath, color = arrowColor)
                }

                if (node.type == NodeType.SINGLE) {
                    node.opts.forEach { o -> o.next?.let { drawEdge(it, selected = false) } }
                }
                node.def?.let { drawEdge(it, selected = false) }
                node.conditions.forEach { c -> drawEdge(c.dest, selected = false) }
            }

            // Draw nodes
            nodes.forEach { (id, node) ->
                val rect = layout[id] ?: return@forEach
                val rx = cx + (rect.x + tx) * scale
                val ry = cy + (rect.y + ty) * scale
                val rw = NW * scale
                val rh = NH * scale
                val cornerR = 6f * scale
                val isSel = id == selectedNodeId

                // Node background
                val fillColor = if (isSel) Color(0xFF0D1A02) else surfColor
                val strokeColor = if (isSel) accentColor else borderColor
                val strokeW = if (isSel) 1.5f else 1f

                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(rx, ry),
                    size = Size(rw, rh),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
                )
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(rx, ry),
                    size = Size(rw, rh),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR),
                    style = Stroke(width = strokeW * scale)
                )

                // Node type badge color
                val badgeColor = when (node.type) {
                    NodeType.SINGLE -> accentColor
                    NodeType.MULTI  -> accent3Color
                    NodeType.TEXT   -> accent5Color
                    NodeType.NONE   -> mutedColor
                }

                // Type indicator dot
                drawCircle(
                    color = badgeColor,
                    radius = 3f * scale,
                    center = Offset(rx + rw - 12f * scale, ry + rh / 2)
                )

                // Node label
                val fontSize = (10f * scale).coerceIn(8f, 16f)
                val maxChars = (NW / 8).toInt()
                val label = if (id.length > maxChars) id.take(maxChars - 1) + "…" else id

                val measured = textMeasurer.measure(
                    AnnotatedString(label),
                    style = TextStyle(
                        color = if (isSel) accentColor else textColor,
                        fontSize = fontSize.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    constraints = Constraints(maxWidth = (rw - 24f * scale).toInt().coerceAtLeast(1))
                )
                drawText(
                    measured,
                    topLeft = Offset(
                        rx + 10f * scale,
                        ry + (rh - measured.size.height) / 2
                    )
                )

                // Opt count label
                val optCount = node.opts.size
                if (optCount > 0) {
                    val countLabel = "$optCount opt${if (optCount != 1) "s" else ""}"
                    val countMeasured = textMeasurer.measure(
                        AnnotatedString(countLabel),
                        style = TextStyle(
                            color = muted2Color,
                            fontSize = (8f * scale).coerceIn(6f, 12f).sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    drawText(
                        countMeasured,
                        topLeft = Offset(
                            rx + 10f * scale,
                            ry + rh - countMeasured.size.height - 6f * scale
                        )
                    )
                }
            }
        }

        // Empty state
        if (nodes.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.Text(
                        "no tree yet",
                        color = WTColors.Muted2,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    androidx.compose.material3.Text(
                        "write nodes in the editor",
                        color = WTColors.Muted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
