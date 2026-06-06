package com.wisdomtree.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtree.parser.WisdomTreeParser
import com.wisdomtree.ui.theme.WTColors

// ── Syntax highlight ──────────────────────────────────────────────────────────

fun buildHighlightedText(src: String): AnnotatedString {
    val spans = WisdomTreeParser.highlight(src)
    return buildAnnotatedString {
        append(src)
        spans.forEach { span ->
            val color = when (span.type) {
                WisdomTreeParser.SpanType.TITLE   -> WTColors.Accent
                WisdomTreeParser.SpanType.ARROW   -> WTColors.Accent4
                WisdomTreeParser.SpanType.DEST    -> WTColors.Accent3
                WisdomTreeParser.SpanType.OPTION  -> WTColors.Text
                WisdomTreeParser.SpanType.COMMENT -> WTColors.Muted
                WisdomTreeParser.SpanType.SET     -> WTColors.Accent5
                WisdomTreeParser.SpanType.IF      -> WTColors.Accent4
                WisdomTreeParser.SpanType.INLINE  -> WTColors.Accent3
            }
            if (span.start < src.length && span.end <= src.length && span.start < span.end) {
                addStyle(SpanStyle(color = color), span.start, span.end)
            }
        }
    }
}

// ── Editor composable ─────────────────────────────────────────────────────────

@Composable
fun EditorScreen(
    source: String,
    onSourceChange: (String) -> Unit,
    nodeCount: Int,
    errors: List<String>,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(buildHighlightedText(source)))
    }

    LaunchedEffect(source) {
        if (source != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                annotatedString = buildHighlightedText(source)
            )
        }
    }

    val lines = source.split("\n")
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Column(modifier = modifier.background(WTColors.Bg)) {

        // ── Status bar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(WTColors.Surf)
                .padding(horizontal = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                "source",
                color = WTColors.Muted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Default,
                letterSpacing = 0.12.sp
            )
            Spacer(Modifier.weight(1f))
            when {
                source.isEmpty() -> Text("—", color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                errors.isNotEmpty() -> Text(
                    "${errors.size} error${if (errors.size > 1) "s" else ""}",
                    color = WTColors.Accent2,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                else -> Text(
                    "$nodeCount node${if (nodeCount != 1) "s" else ""} ✓",
                    color = WTColors.Accent,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // ── Editor body ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .weight(1f)
                .background(WTColors.Surf)
        ) {
            // Line numbers
            Column(
                modifier = Modifier
                    .width(34.dp)
                    .fillMaxHeight()
                    .verticalScroll(verticalScroll)
                    .padding(top = 14.dp, bottom = 14.dp, end = 7.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.End
            ) {
                lines.forEachIndexed { i, _ ->
                    Text(
                        text = "${i + 1}",
                        color = WTColors.Muted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 21.sp
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(WTColors.Border)
            )

            // Text field
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newVal ->
                    val newSrc = newVal.text
                    val highlighted = buildHighlightedText(newSrc)
                    textFieldValue = newVal.copy(annotatedString = highlighted)
                    onSourceChange(newSrc)
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScroll)
                    .padding(14.dp),
                textStyle = TextStyle(
                    color = WTColors.Text,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 21.sp
                ),
                cursorBrush = SolidColor(WTColors.Accent),
                decorationBox = { innerTextField ->
                    if (source.isEmpty()) {
                        Text(
                            text = "Did you achieve what you set out to do?\n  Yes >> raise the bar\n  No >> what was the problem?\n\nwhat was the problem?\n  I gave up\n  had no choice\n  >> what would improve tomorrow?\n\nwhat would improve tomorrow?\n  >> done",
                            color = WTColors.Muted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 21.sp
                        )
                    }
                    innerTextField()
                }
            )
        }

        // ── Schema hint bar ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WTColors.Surf)
                .padding(horizontal = 14.dp, vertical = 9.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            SchemaChip("Title", WTColors.Accent)
            SchemaText("  ·  ")
            SchemaChip("Opt", WTColors.Accent)
            SchemaText(" ")
            SchemaChip(">>", WTColors.Accent4)
            SchemaText(" ")
            SchemaChip("dest", WTColors.Accent3)
            SchemaText("  ·  bare opts + ")
            SchemaChip(">>", WTColors.Accent4)
            SchemaText(" ")
            SchemaChip("dest", WTColors.Accent3)
            SchemaText(" = multi  ·  ")
            SchemaChip(">>", WTColors.Accent4)
            SchemaText(" ")
            SchemaChip("dest", WTColors.Accent3)
            SchemaText(" alone = text  ·  ")
            SchemaChip("@set", WTColors.Accent5)
            SchemaText(" key op val")
        }
    }
}

@Composable
private fun SchemaChip(text: String, color: Color) {
    Text(text, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
}

@Composable
private fun SchemaText(text: String) {
    Text(text, color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
}
