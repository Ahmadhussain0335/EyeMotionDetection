@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.app.eyesmotiondetection.ui.screens.preview

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.app.eyesmotiondetection.detection.model.GestureResult
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

private fun gazeLabel(yaw: Float, pitch: Float): String = when {
    yaw > 15f -> "Looking Left"
    yaw < -15f -> "Looking Right"
    pitch > 10f -> "Looking Down"
    pitch < -10f -> "Looking Up"
    else -> "Center"
}

@Composable
fun LivePreviewScreen(
    onBack: () -> Unit,
    viewModel: LivePreviewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recentGestures by viewModel.recentGestures.collectAsState()
    val latestGesture by viewModel.latestGesture.collectAsState()
    val faceData by viewModel.faceData.collectAsState()

    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    // Banner auto-hides 3 s after the last detection
    var visibleGesture by remember { mutableStateOf<GestureResult?>(null) }
    LaunchedEffect(latestGesture) {
        if (latestGesture != null) {
            visibleGesture = latestGesture
            delay(3000)
            visibleGesture = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eye Training") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (recentGestures.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearHistory) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear history")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Zone 1: Camera + live metrics overlay ──────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        // PreviewView scales its inner view to fill; without this it draws over later items
                        .clipToBounds()
                        .background(Color.Black)
                ) {
                    if (hasCameraPermission) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    // Bind once; update would rebind on every recomposition it observes
                                    viewModel.startCamera(lifecycleOwner, surfaceProvider)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Camera permission required",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    // Live metrics overlay (bottom of camera box)
                    faceData?.let { fd ->
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Face detected badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (fd.faceDetected) Color(0xFF00E676) else Color(0xFFFF5252)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (fd.faceDetected) "Face detected" else "No face",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                                if (fd.faceDetected) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        gazeLabel(fd.headEulerAngleY, fd.headEulerAngleX),
                                        color = Color(0xFF80D8FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (fd.faceDetected) {
                                EyeMetricRow(
                                    label = "L Eye",
                                    prob = fd.leftEyeOpenProbability,
                                    ear = fd.leftEyeEAR
                                )
                                EyeMetricRow(
                                    label = "R Eye",
                                    prob = fd.rightEyeOpenProbability,
                                    ear = fd.rightEyeEAR
                                )
                            }
                        }
                    }
                }
            }

            // ── Zone 2: Gesture detection banner ───────────────────────────
            item {
                AnimatedVisibility(
                    visible = visibleGesture != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    visibleGesture?.let { g ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = g.gesture.displayName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Confidence: ${"%.0f".format(g.confidence * 100)}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Placeholder when nothing is firing
                if (visibleGesture == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Waiting for gesture… blink, wink or look around",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Zone 3: History header ──────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Detection History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (recentGestures.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "(${recentGestures.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Zone 3: History list or empty state ─────────────────────────
            if (recentGestures.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No gestures yet — perform a blink, wink,\nlook left/right, or open your eyes wide",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(recentGestures.reversed()) { result ->
                    GestureHistoryRow(result)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun EyeMetricRow(label: String, prob: Float, ear: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, modifier = Modifier.width(36.dp))
        LinearProgressIndicator(
            progress = { prob.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when {
                prob < 0.3f -> Color(0xFFFF5252)
                prob < 0.6f -> Color(0xFFFFD740)
                else -> Color(0xFF00E676)
            },
            trackColor = Color.White.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "${"%.0f".format(prob * 100)}%",
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier.width(32.dp)
        )
        Text(
            "EAR ${"%.2f".format(ear)}",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            modifier = Modifier.width(54.dp)
        )
    }
}

@Composable
private fun GestureHistoryRow(result: GestureResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(result.gesture.displayName, fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            "${"%.0f".format(result.confidence * 100)}%",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Text(
            sdf.format(Date(result.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
