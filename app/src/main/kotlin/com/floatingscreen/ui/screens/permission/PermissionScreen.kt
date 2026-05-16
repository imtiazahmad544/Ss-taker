package com.floatingscreen.ui.screens.permission

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.floatingscreen.ui.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

data class PermissionItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val isRequired: Boolean,
    val actionLabel: String,
    val onAction: () -> Unit
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    mainViewModel: MainViewModel,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val permissionStatus by mainViewModel.permissionStatus.collectAsStateWithLifecycle()

    // Runtime permissions
    val runtimePermissions = rememberMultiplePermissionsState(
        mainViewModel.permissionManager.getRequiredRuntimePermissions().toList()
    )

    // Re-check on resume
    LaunchedEffect(Unit) {
        mainViewModel.refreshPermissions()
    }

    val allGranted = permissionStatus.overlayPermission &&
            permissionStatus.notificationPermission &&
            permissionStatus.recordAudioPermission

    val permissionItems = buildPermissionItems(
        context = context,
        permissionStatus = permissionStatus,
        onRequestRuntime = { runtimePermissions.launchMultiplePermissionRequest() },
        onRefresh = { mainViewModel.refreshPermissions() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Required Permissions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (allGranted) {
                        IconButton(onClick = onPermissionsGranted) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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

            // Header
            PermissionHeader(allGranted = allGranted)

            // Permission list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(permissionItems) { item ->
                    PermissionCard(item = item)
                }

                // Media projection note
                item {
                    MediaProjectionNote()
                }
            }

            // Bottom CTA
            AnimatedVisibility(visible = allGranted) {
                Button(
                    onClick = onPermissionsGranted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("All Set — Go to App", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionHeader(allGranted: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (allGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (allGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (allGranted) "All Permissions Granted" else "Permissions Required",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (allGranted)
                    "You're ready to use Floating Screen Utility"
                else
                    "These permissions are needed for screen recording and the floating overlay window. All permissions are used only for core functionality.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PermissionCard(item: PermissionItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isGranted)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = if (item.isGranted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (item.isRequired) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "Required",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    item.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action
            if (item.isGranted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                TextButton(onClick = item.onAction) {
                    Text(item.actionLabel, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MediaProjectionNote() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Info,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Column {
                Text(
                    "Screen Capture Permission",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Android shows a system dialog the first time you start a recording or screenshot. This is required by the OS and cannot be bypassed — it ensures you have explicit control over screen sharing.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun buildPermissionItems(
    context: android.content.Context,
    permissionStatus: com.floatingscreen.domain.model.PermissionStatus,
    onRequestRuntime: () -> Unit,
    onRefresh: () -> Unit
): List<PermissionItem> = buildList {
    add(
        PermissionItem(
            title = "Display Over Other Apps",
            description = "Required for the floating recording controls to appear on top of other applications.",
            icon = Icons.Default.Layers,
            isGranted = permissionStatus.overlayPermission,
            isRequired = true,
            actionLabel = "Grant",
            onAction = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
                onRefresh()
            }
        )
    )

    add(
        PermissionItem(
            title = "Microphone",
            description = "Optional: record audio alongside your screen capture.",
            icon = Icons.Default.Mic,
            isGranted = permissionStatus.recordAudioPermission,
            isRequired = false,
            actionLabel = "Grant",
            onAction = onRequestRuntime
        )
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(
            PermissionItem(
                title = "Notifications",
                description = "Required to show the foreground service notification while recording.",
                icon = Icons.Default.Notifications,
                isGranted = permissionStatus.notificationPermission,
                isRequired = true,
                actionLabel = "Grant",
                onAction = onRequestRuntime
            )
        )

        add(
            PermissionItem(
                title = "Media Access (Photos & Videos)",
                description = "Read saved recordings and screenshots in your gallery.",
                icon = Icons.Default.PhotoLibrary,
                isGranted = true, // READ_MEDIA_VIDEO/IMAGES are checked elsewhere
                isRequired = false,
                actionLabel = "Grant",
                onAction = onRequestRuntime
            )
        )
    } else {
        add(
            PermissionItem(
                title = "Storage",
                description = "Save recordings and screenshots to your device.",
                icon = Icons.Default.SdStorage,
                isGranted = true,
                isRequired = false,
                actionLabel = "Grant",
                onAction = onRequestRuntime
            )
        )
    }
}
