package com.wisdomtree.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.wisdomtree.notifications.NotificationScheduler
import com.wisdomtree.ui.theme.WTColors
import java.io.OutputStreamWriter

@Composable
fun SettingsScreen(
    treeSource: String,
    onImportSource: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // ── Notification state ────────────────────────────────────────────────────
    val (initEnabled, initHour, initMinute) = remember { NotificationScheduler.getSettings(context) }
    var notifEnabled by remember { mutableStateOf(initEnabled) }
    var notifHour by remember { mutableStateOf(initHour) }
    var notifMinute by remember { mutableStateOf(initMinute) }
    var showTimePicker by remember { mutableStateOf(false) }
    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted && notifEnabled) NotificationScheduler.schedule(context, notifHour, notifMinute)
    }

    // ── Export launcher ───────────────────────────────────────────────────────
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                OutputStreamWriter(os).use { w -> w.write(treeSource) }
            }
        }
    }

    // ── Import launcher ───────────────────────────────────────────────────────
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { ins ->
                val src = ins.bufferedReader().readText()
                onImportSource(src)
            }
        }
    }

    // ── Share (export via share sheet) ────────────────────────────────────────
    fun shareTree() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, treeSource)
            putExtra(Intent.EXTRA_SUBJECT, "Wisdom Tree")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Tree"))
    }

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("settings", color = WTColors.Accent4, fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {

            // ── Notifications section ─────────────────────────────────────────
            SettingsSection(title = "notifications") {
                // Permission banner
                if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1A1000))
                            .border(BorderStroke(1.dp, WTColors.Accent4), RoundedCornerShape(6.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notification permission needed", color = WTColors.Accent4, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WTColors.Accent4)
                                .clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) { Text("grant", color = Color.Black, fontSize = 10.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // Enable toggle
                SettingsRow(
                    label = "daily reminder",
                    sublabel = "Get a notification to run your tree"
                ) {
                    Switch(
                        checked = notifEnabled,
                        onCheckedChange = { enabled ->
                            notifEnabled = enabled
                            if (enabled) {
                                if (hasNotifPermission) {
                                    NotificationScheduler.schedule(context, notifHour, notifMinute)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                NotificationScheduler.cancel(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = WTColors.Accent,
                            uncheckedThumbColor = WTColors.Muted,
                            uncheckedTrackColor = WTColors.Surf2
                        )
                    )
                }

                // Time picker
                if (notifEnabled) {
                    Spacer(Modifier.height(4.dp))
                    SettingsRow(
                        label = "reminder time",
                        sublabel = formatTime(notifHour, notifMinute)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WTColors.Surf2)
                                .border(BorderStroke(1.dp, WTColors.Border2), RoundedCornerShape(4.dp))
                                .clickable { showTimePicker = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(formatTime(notifHour, notifMinute), color = WTColors.Accent, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // ── Tree export/import section ────────────────────────────────────
            SettingsSection(title = "tree file") {
                SettingsRow(
                    label = "export tree",
                    sublabel = "Save your tree source as a .txt file"
                ) {
                    ActionButton(
                        label = "export",
                        color = WTColors.Accent3,
                        dimBg = WTColors.Accent3Dim,
                        onClick = { exportLauncher.launch("wisdom-tree.txt") }
                    )
                }

                Spacer(Modifier.height(4.dp))

                SettingsRow(
                    label = "share tree",
                    sublabel = "Send tree source via any app"
                ) {
                    ActionButton(
                        label = "share",
                        color = WTColors.Accent3,
                        dimBg = WTColors.Accent3Dim,
                        onClick = { shareTree() }
                    )
                }

                Spacer(Modifier.height(4.dp))

                SettingsRow(
                    label = "import tree",
                    sublabel = "Load a tree from a .txt file"
                ) {
                    ActionButton(
                        label = "import",
                        color = WTColors.Accent4,
                        dimBg = Color(0xFF1A1000),
                        onClick = { importLauncher.launch(arrayOf("text/plain", "*/*")) }
                    )
                }
            }
        }
    }

    // ── Time picker dialog ────────────────────────────────────────────────────
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = notifHour,
            initialMinute = notifMinute,
            onConfirm = { h, m ->
                notifHour = h
                notifMinute = m
                showTimePicker = false
                if (notifEnabled) NotificationScheduler.schedule(context, h, m)
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = WTColors.Muted, fontSize = 10.sp, fontFamily = FontFamily.Default, letterSpacing = 0.14.sp)
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(WTColors.Surf)
                .border(BorderStroke(1.dp, WTColors.Border2), RoundedCornerShape(8.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsRow(label: String, sublabel: String, action: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, color = WTColors.Text, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Text(sublabel, color = WTColors.Muted2, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        action()
    }
}

@Composable
private fun ActionButton(label: String, color: Color, dimBg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(dimBg)
            .border(BorderStroke(1.dp, color), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, letterSpacing = 0.06.sp)
    }
}

@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WTColors.Surf,
        titleContentColor = WTColors.Text,
        textContentColor = WTColors.Text,
        title = { Text("Set reminder time", fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Hour: $hour", color = WTColors.Muted2, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Slider(
                    value = hour.toFloat(),
                    onValueChange = { hour = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 22,
                    colors = SliderDefaults.colors(thumbColor = WTColors.Accent, activeTrackColor = WTColors.Accent)
                )
                Text("Minute: ${minute.toString().padStart(2, '0')}", color = WTColors.Muted2, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Slider(
                    value = minute.toFloat(),
                    onValueChange = { minute = it.toInt() },
                    valueRange = 0f..59f,
                    steps = 58,
                    colors = SliderDefaults.colors(thumbColor = WTColors.Accent, activeTrackColor = WTColors.Accent)
                )
                Text(
                    "Reminder set for ${formatTime(hour, minute)} daily",
                    color = WTColors.Accent, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(hour, minute) },
                colors = ButtonDefaults.buttonColors(containerColor = WTColors.Accent, contentColor = Color.Black)
            ) { Text("set", fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("cancel", color = WTColors.Muted, fontSize = 11.sp) }
        }
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return "$h:${minute.toString().padStart(2, '0')} $amPm"
}
