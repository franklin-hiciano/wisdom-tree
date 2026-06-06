package com.wisdomtree

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
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
import androidx.core.view.WindowCompat
import com.wisdomtree.notifications.NotificationScheduler
import com.wisdomtree.ui.screens.canvas.TreeCanvas
import com.wisdomtree.ui.screens.editor.EditorScreen
import com.wisdomtree.ui.screens.nodeeditor.NodeEditorScreen
import com.wisdomtree.ui.screens.runs.RunsScreen
import com.wisdomtree.ui.screens.settings.SettingsScreen
import com.wisdomtree.ui.screens.test.TestScreen
import com.wisdomtree.ui.screens.treestats.TreeStatsScreen
import com.wisdomtree.ui.screens.vars.VarsScreen
import com.wisdomtree.ui.theme.WTColors
import com.wisdomtree.ui.theme.WisdomTreeTheme
import com.wisdomtree.viewmodel.WisdomTreeViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: WisdomTreeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        NotificationScheduler.createChannel(this)
        setContent {
            WisdomTreeTheme {
                WisdomTreeApp(vm = vm)
            }
        }
    }
}

enum class Overlay { NONE, RUNS, VARS, SETTINGS, NODE_EDITOR, TREE_STATS }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WisdomTreeApp(vm: WisdomTreeViewModel) {
    val editorState by vm.editor.collectAsState()
    val canvasState by vm.canvas.collectAsState()
    val testState   by vm.test.collectAsState()
    val runsState   by vm.runs.collectAsState()
    val varsState   by vm.vars.collectAsState()

    var overlay by remember { mutableStateOf(Overlay.NONE) }
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WTColors.Bg)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(WTColors.Bg)
                    .border(BorderStroke(1.dp, WTColors.Border2))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("wisdom tree", color = WTColors.Accent,
                    fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp, letterSpacing = 0.04.sp)

                Divider()

                // Pane tabs
                TopPill("editor", pagerState.currentPage == 0) { scope.launch { pagerState.animateScrollToPage(0) } }
                TopPill("canvas", pagerState.currentPage == 1) { scope.launch { pagerState.animateScrollToPage(1) } }

                Divider()

                // Node editor
                TopPill("nodes", color = WTColors.Accent5, dimBg = WTColors.Accent5Dim) {
                    overlay = Overlay.NODE_EDITOR
                }

                Divider()

                // Stats / Runs / Vars
                TopPill("stats", color = WTColors.Accent4, dimBg = Color(0xFF1A1000)) {
                    vm.loadRuns("all"); overlay = Overlay.TREE_STATS
                }
                TopPill("runs", color = WTColors.Accent3, dimBg = WTColors.Accent3Dim) {
                    vm.loadRuns(); overlay = Overlay.RUNS
                }
                TopPill("vars", color = WTColors.Accent5, dimBg = WTColors.Accent5Dim) {
                    vm.loadVars(); overlay = Overlay.VARS
                }

                Divider()

                // Test button
                TopPill("▶ test", color = WTColors.Accent, dimBg = WTColors.AccentDim) {
                    vm.enterTest()
                }

                // Settings
                TopPill("⚙", color = WTColors.Muted2) { overlay = Overlay.SETTINGS }

                // Reset
                TopPill("↺", color = WTColors.Muted2) { vm.resetSource() }
            }

            // ── Pager: Editor / Canvas ────────────────────────────────────────
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                when (page) {
                    0 -> EditorScreen(
                        source = editorState.source,
                        onSourceChange = vm::updateSource,
                        nodeCount = editorState.nodeCount,
                        errors = editorState.errors,
                        modifier = Modifier.fillMaxSize()
                    )
                    1 -> Column(modifier = Modifier.fillMaxSize().background(WTColors.Bg)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(30.dp)
                                .background(WTColors.Surf).border(BorderStroke(1.dp, WTColors.Border))
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("canvas", color = WTColors.Muted, fontSize = 10.sp,
                                fontFamily = FontFamily.Default, letterSpacing = 0.12.sp)
                            Spacer(Modifier.weight(1f))
                            Text("pinch to zoom · drag to pan · tap to select",
                                color = WTColors.Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                        TreeCanvas(
                            nodes = canvasState.nodes,
                            selectedNodeId = canvasState.selectedNodeId,
                            onNodeTap = { vm.selectNode(it) },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
            }
        }

        // ── Overlays (slide up from bottom) ───────────────────────────────────

        SlideOverlay(testState.active) {
            TestScreen(
                state = testState,
                currentNode = testState.currentNodeId?.let { vm.getNode(it) },
                onBack = vm::goBack,
                onExit = vm::exitTest,
                onAnswerSingle = vm::answerSingle,
                onAnswerMulti = vm::answerMulti,
                onAnswerText = vm::answerText,
                onSaveRun = vm::showSaveBanner,
                onConfirmSave = vm::confirmSaveRun,
                onDismissSave = vm::dismissSaveBanner,
                modifier = Modifier.fillMaxSize()
            )
        }

        SlideOverlay(overlay == Overlay.NODE_EDITOR) {
            Column(modifier = Modifier.fillMaxSize().background(WTColors.Bg)) {
                OverlayHeader("node editor", WTColors.Accent5) { overlay = Overlay.NONE }
                NodeEditorScreen(
                    nodes = canvasState.nodes,
                    onSourceChange = vm::updateSource,
                    onClose = { overlay = Overlay.NONE },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SlideOverlay(overlay == Overlay.TREE_STATS) {
            TreeStatsScreen(
                runs = runsState.runs,
                nodes = canvasState.nodes,
                onClose = { overlay = Overlay.NONE },
                modifier = Modifier.fillMaxSize()
            )
        }

        SlideOverlay(overlay == Overlay.RUNS) {
            RunsScreen(
                state = runsState,
                onFilterChange = vm::setRunsFilter,
                onClose = { overlay = Overlay.NONE },
                modifier = Modifier.fillMaxSize()
            )
        }

        SlideOverlay(overlay == Overlay.VARS) {
            VarsScreen(
                state = varsState,
                onSetOverride = vm::setVarOverride,
                onClose = { overlay = Overlay.NONE },
                modifier = Modifier.fillMaxSize()
            )
        }

        SlideOverlay(overlay == Overlay.SETTINGS) {
            SettingsScreen(
                treeSource = editorState.source,
                onImportSource = { src -> vm.updateSource(src); overlay = Overlay.NONE },
                onClose = { overlay = Overlay.NONE },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SlideOverlay(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit  = slideOutVertically(targetOffsetY = { it })
    ) { content() }
}

@Composable
private fun OverlayHeader(title: String, color: Color, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(WTColors.Bg)
            .border(BorderStroke(1.dp, WTColors.Border2))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = color, fontFamily = FontFamily.Default,
            fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = onClose,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = WTColors.Muted2),
            border = BorderStroke(1.dp, WTColors.Border2),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(28.dp)
        ) { Text("esc", fontSize = 10.sp, fontFamily = FontFamily.Default) }
    }
}

@Composable
private fun RowScope.Divider() {
    Box(modifier = Modifier.width(1.dp).height(14.dp).background(WTColors.Border2))
}

@Composable
private fun TopPill(
    label: String,
    selected: Boolean = false,
    color: Color = WTColors.Muted2,
    dimBg: Color = Color.Transparent,
    onClick: () -> Unit
) {
    val effectiveColor  = if (selected) WTColors.Accent else color
    val effectiveBorder = if (selected) WTColors.Accent
                          else if (dimBg != Color.Transparent) dimBg.copy(alpha = 1f)
                          else WTColors.Border2
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (dimBg != Color.Transparent) dimBg else Color.Transparent)
            .border(BorderStroke(1.dp, effectiveBorder), RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(label, color = effectiveColor, fontSize = 10.sp,
            fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, letterSpacing = 0.08.sp)
    }
}
