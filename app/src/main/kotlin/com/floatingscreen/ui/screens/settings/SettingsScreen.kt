package com.floatingscreen.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floatingscreen.domain.model.*
import com.floatingscreen.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ─── Recording Quality ───────────────────────────────────────────
            item {
                SettingsSection(title = "Recording", icon = Icons.Default.Videocam) {
                    DropdownSetting(
                        label = "Video Quality",
                        subtitle = "Resolution of recorded video",
                        options = RecordingQuality.values().map { it.label },
                        selected = settings.recordingQuality.label,
                        onSelect = { label ->
                            mainViewModel.updateSettings(
                                settings.copy(recordingQuality = RecordingQuality.fromLabel(label))
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownSetting(
                        label = "Frame Rate",
                        subtitle = "Frames per second",
                        options = FrameRate.values().map { it.label },
                        selected = settings.frameRate.label,
                        onSelect = { label ->
                            mainViewModel.updateSettings(
                                settings.copy(frameRate = FrameRate.values().find { it.label == label }
                                    ?: FrameRate.FPS_30)
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownSetting(
                        label = "Audio Source",
                        subtitle = "Audio captured during recording",
                        options = AudioSource.values().map { it.label },
                        selected = settings.audioSource.label,
                        onSelect = { label ->
                            mainViewModel.updateSettings(
                                settings.copy(audioSource = AudioSource.fromLabel(label))
                            )
                        }
                    )
                }
            }

            // ─── Storage ─────────────────────────────────────────────────────
            item {
                SettingsSection(title = "Storage", icon = Icons.Default.Folder) {
                    DropdownSetting(
                        label = "Save Location",
                        subtitle = "Where media files are saved",
                        options = SaveLocation.values().map { it.label },
                        selected = settings.saveLocation.label,
                        onSelect = { label ->
                            mainViewModel.updateSettings(
                                settings.copy(saveLocation = SaveLocation.fromLabel(label))
                            )
                        }
                    )
                }
            }

            // ─── Floating Window ─────────────────────────────────────────────
            item {
                SettingsSection(title = "Floating Window", icon = Icons.Default.OpenWith) {
                    SliderSetting(
                        label = "Button Opacity",
                        subtitle = "Transparency of floating bubble",
                        value = settings.floatingButtonOpacity,
                        onValueChange = { opacity ->
                            mainViewModel.updateSettings(
                                settings.copy(floatingButtonOpacity = opacity)
                            )
                        },
                        valueRange = 0.3f..1.0f,
                        steps = 7,
                        displayValue = "${(settings.floatingButtonOpacity * 100).toInt()}%"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SwitchSetting(
                        label = "Auto-Start Floating Widget",
                        subtitle = "Show floating bubble on device boot",
                        checked = settings.autoStartFloating,
                        onCheckedChange = { enabled ->
                            mainViewModel.updateSettings(
                                settings.copy(autoStartFloating = enabled)
                            )
                        }
                    )
                }
            }

            // ─── Appearance ──────────────────────────────────────────────────
            item {
                SettingsSection(title = "Appearance", icon = Icons.Default.Palette) {
                    SwitchSetting(
                        label = "Dark Theme",
                        subtitle = "Use dark color scheme",
                        checked = settings.isDarkTheme,
                        onCheckedChange = { dark ->
                            mainViewModel.updateSettings(settings.copy(isDarkTheme = dark))
                        }
                    )
                }
            }

            // ─── Behaviour ───────────────────────────────────────────────────
            item {
                SettingsSection(title = "Behaviour", icon = Icons.Default.Tune) {
                    SwitchSetting(
                        label = "Countdown Timer",
                        subtitle = "Show countdown before delayed screenshots",
                        checked = settings.showCountdownTimer,
                        onCheckedChange = { show ->
                            mainViewModel.updateSettings(settings.copy(showCountdownTimer = show))
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SwitchSetting(
                        label = "Vibration Feedback",
                        subtitle = "Vibrate on capture start/stop",
                        checked = settings.vibrateFeedback,
                        onCheckedChange = { vib ->
                            mainViewModel.updateSettings(settings.copy(vibrateFeedback = vib))
                        }
                    )
                }
            }

            // ─── About ───────────────────────────────────────────────────────
            item {
                SettingsSection(title = "About", icon = Icons.Default.Info) {
                    AboutRow(label = "Version", value = "1.0.0")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    AboutRow(label = "Build", value = "Production")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    AboutRow(label = "Min SDK", value = "Android 10 (API 29)")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    AboutRow(label = "Target SDK", value = "Android 15 (API 35)")
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Section wrapper
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Individual setting row types
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SwitchSetting(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DropdownSetting(
    label: String,
    subtitle: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(selected, fontSize = 12.sp, maxLines = 1)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = { onSelect(option); expanded = false },
                        trailingIcon = if (option == selected) {
                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                displayValue,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}
