@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.app.eyesmotiondetection.ui.screens.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun PermissionScreen(
    onPermissionsGranted: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh whenever the screen resumes (e.g. coming back from Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }

    LaunchedEffect(state.allGranted) { if (state.allGranted) onPermissionsGranted() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Setup Permissions") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "EyeControl needs the following permissions to function:",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                PermissionItem(
                    title = "Camera",
                    description = "Detect eye movements via front camera",
                    granted = state.cameraGranted,
                    onRequest = { cameraLauncher.launch(Manifest.permission.CAMERA) }
                )

                PermissionItem(
                    title = "Display Over Other Apps",
                    description = "Show eye tracking indicator on screen",
                    granted = state.overlayGranted,
                    onRequest = { viewModel.openOverlaySettings(context) }
                )

                PermissionItem(
                    title = "Accessibility Service",
                    description = "Perform gestures and control phone",
                    granted = state.accessibilityEnabled,
                    onRequest = { viewModel.openAccessibilitySettings(context) }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionItem(
                        title = "Notifications (optional)",
                        description = "Status while tracking, and a prompt to resume after restart",
                        granted = state.notificationsGranted,
                        onRequest = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                }

                // Always-visible step-by-step guide when accessibility is not yet enabled
                if (!state.accessibilityEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "How to enable the Accessibility Service:",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text("1. Tap Grant above → Android Accessibility Settings opens", style = MaterialTheme.typography.bodySmall)
                            Text("2. Scroll down to \"Downloaded apps\" section", style = MaterialTheme.typography.bodySmall)
                            Text("3. Tap \"Eye Gesture Control\"", style = MaterialTheme.typography.bodySmall)
                            Text("4. Toggle the switch ON", style = MaterialTheme.typography.bodySmall)
                            Text("5. Tap Allow in the confirmation dialog", style = MaterialTheme.typography.bodySmall)
                            Text("6. Press Back to return here — status updates automatically", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.refresh() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Refresh Status") }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!granted) {
                TextButton(onClick = onRequest) { Text("Grant") }
            }
        }
    }
}
