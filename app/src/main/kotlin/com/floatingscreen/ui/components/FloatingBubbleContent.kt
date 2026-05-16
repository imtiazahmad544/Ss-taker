package com.floatingscreen.ui.components

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Composable representing the floating bubble controls.
 * This is intended to be hosted inside a ComposeView added to WindowManager.
 */
@Composable
fun FloatingBubbleContent(
    isExpanded: Boolean,
    isRecording: Boolean,
    opacity: Float,
    onToggleExpand: () -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onScreenshot: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bubble_scale"
    )

    // Pulsing red dot when recording
    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            tween(700, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "pulse_dot"
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Expanded controls panel
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }) + expandVertically(),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }) + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .width(180.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = opacity)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isRecording) {
                        FlatAction(
                            icon = Icons.Default.FiberManualRecord,
                            label = "Record",
                            tint = Color(0xFFE53935),
                            onClick = onStartRecord
                        )
                    } else {
                        FlatAction(
                            icon = Icons.Default.Stop,
                            label = "Stop",
                            tint = Color(0xFFE53935),
                            onClick = onStopRecord
                        )
                    }
                    FlatAction(
                        icon = Icons.Default.Screenshot,
                        label = "Screenshot",
                        tint = Color(0xFF43A047),
                        onClick = onScreenshot
                    )
                    HorizontalDivider()
                    FlatAction(
                        icon = Icons.Default.VideoLibrary,
                        label = "History",
                        onClick = onOpenHistory
                    )
                    FlatAction(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = onOpenSettings
                    )
                    HorizontalDivider()
                    FlatAction(
                        icon = Icons.Default.Close,
                        label = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onClose
                    )
                }
            }
        }

        // Main draggable bubble
        Box(contentAlignment = Alignment.Center) {
            FloatingActionButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = opacity),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.OpenWith,
                    contentDescription = "Toggle",
                    tint = Color.White
                )
            }

            // Recording indicator dot
            AnimatedVisibility(
                visible = isRecording,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                )
            }
        }
    }
}

@Composable
private fun FlatAction(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontSize = 13.sp,
            color = tint,
            modifier = Modifier.weight(1f)
        )
    }
}
