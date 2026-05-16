package com.floatingscreen.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floatingscreen.domain.model.RecordingState
import com.floatingscreen.domain.model.ScreenshotState
import com.floatingscreen.ui.MainViewModel
import com.floatingscreen.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    val recordingState by mainViewModel.recordingState.collectAsStateWithLifecycle()
    val screenshotState by mainViewModel.screenshotState.collectAsStateWithLifecycle()
    val permissionStatus by mainViewModel.permissionStatus.collectAsStateWithLifecycle()
    val isFloatingVisible by mainViewModel.isFloatingVisible.collectAsStateWithLifecycle()

    var showScreenshotDelay by remember { mutableStateOf(false) }

    // Recording timer
    var elapsedSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(recordingState) {
        if (recordingState is RecordingState.Recording) {
            val start = (recordingState as RecordingState.Recording).startTimeMs
            while (recordingState is RecordingState.Recording) {
                elapsedSeconds = (System.currentTimeMillis() - start) / 1000
                delay(1000)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToHistory = onNavigateToHistory
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Permission warning
            if (!permissionStatus.overlayPermission || !permissionStatus.notificationPermission) {
                PermissionWarningCard(onClick = onNavigateToPermissions)
            }

            // Recording status card
            RecordingStatusCard(
                recordingState = recordingState,
                elapsedSeconds = elapsedSeconds,
                onStart = { mainViewModel.startRecording() },
                onStop = { mainViewModel.stopRecording() },
                onPause = { mainViewModel.pauseRecording() },
                onResume = { mainViewModel.resumeRecording() }
            )

            // Screenshot card
            ScreenshotCard(
                screenshotState = screenshotState,
                onCapture = { delay -> mainViewModel.takeScreenshot(delay) },
                showDelayPicker = showScreenshotDelay,
                onToggleDelayPicker = { showScreenshotDelay = !showScreenshotDelay }
            )

            // Floating window card
            FloatingWindowCard(
                isActive = isFloatingVisible,
                onToggle = {
                    if (isFloatingVisible) mainViewModel.hideFloatingWindow()
                    else mainViewModel.showFloatingWindow()
                }
            )

            // Quick actions
            QuickActionsRow(
                onHistory = onNavigateToHistory,
                onSettings = onNavigateToSettings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Floating Screen",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Utility",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            IconButton(onClick = onNavigateToHistory) {
                Icon(Icons.Default.VideoLibrary, contentDescription = "History")
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun PermissionWarningCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Permissions Required",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "Tap to grant required permissions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun RecordingStatusCard(
    recordingState: RecordingState,
    elapsedSeconds: Long,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    val isRecording = recordingState is RecordingState.Recording
    val isPaused = recordingState is RecordingState.Paused

    // Pulsing animation for recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecording) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Screen Recording",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        when (recordingState) {
                            is RecordingState.Idle -> "Ready to record"
                            is RecordingState.Preparing -> "Preparing..."
                            is RecordingState.Recording -> formatDuration(elapsedSeconds)
                            is RecordingState.Paused -> "Paused"
                            is RecordingState.Stopping -> "Saving..."
                            is RecordingState.Completed -> "Saved!"
                            is RecordingState.Error -> "Error"
                        },
                        fontSize = 14.sp,
                        color = if (isRecording) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .scale(pulse)
                            .clip(CircleShape)
                            .background(AccentRed)
                    )
                } else {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    isRecording -> {
                        OutlinedButton(
                            onClick = onPause,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pause")
                        }
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                    isPaused -> {
                        Button(
                            onClick = onResume,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Resume")
                        }
                        OutlinedButton(
                            onClick = onStop,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                    else -> {
                        Button(
                            onClick = onStart,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = recordingState is RecordingState.Idle || recordingState is RecordingState.Completed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.FiberManualRecord, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Start Recording", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotCard(
    screenshotState: ScreenshotState,
    onCapture: (Int) -> Unit,
    showDelayPicker: Boolean,
    onToggleDelayPicker: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Screenshot", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Icon(
                    Icons.Default.Screenshot,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onCapture(0) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Capture Now")
                }
                OutlinedButton(
                    onClick = onToggleDelayPicker,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Timer, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delayed")
                }
            }

            AnimatedVisibility(visible = showDelayPicker) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(3, 5, 10).forEach { seconds ->
                        OutlinedButton(
                            onClick = { onCapture(seconds) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("${seconds}s")
                        }
                    }
                }
            }

            if (screenshotState is ScreenshotState.Countdown) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentGreen
                )
                Text(
                    "Taking screenshot in ${screenshotState.secondsLeft}s...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FloatingWindowCard(isActive: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Floating Window",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    if (isActive) "Active - drag the bubble to move" else "Show controls over other apps",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
private fun QuickActionsRow(onHistory: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(
            icon = Icons.Default.VideoLibrary,
            label = "History",
            onClick = onHistory,
            modifier = Modifier.weight(1f)
        )
        QuickActionCard(
            icon = Icons.Default.Settings,
            label = "Settings",
            onClick = onSettings,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    val h = m / 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m % 60, s)
    else "%02d:%02d".format(m, s)
}
