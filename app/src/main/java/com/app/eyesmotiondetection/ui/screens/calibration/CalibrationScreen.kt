@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.app.eyesmotiondetection.ui.screens.calibration

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.app.eyesmotiondetection.domain.model.DetectionSettings
import kotlin.math.roundToInt

@Composable
fun CalibrationScreen(
    onBack: () -> Unit,
    viewModel: CalibrationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CameraBox(
                hasCameraPermission = hasCameraPermission,
                faceDetected = state.faceDetected,
                onBind = { viewModel.bindCamera(lifecycleOwner, it) }
            )

            WizardCard(
                state = state,
                canStart = hasCameraPermission,
                onStart = viewModel::startCalibration,
                onCancel = viewModel::cancelCalibration
            )

            HorizontalDivider()

            // Manual threshold controls
            Text("Manual Adjustments", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))

            ThresholdSlider(
                label = "Blink Sensitivity",
                value = settings.blinkThreshold,
                range = 0.1f..0.6f,
                onValueChange = { viewModel.updateBlinkThreshold(it) },
                valueDisplay = { "%.2f".format(it) }
            )

            ThresholdSlider(
                label = "Gaze Angle (°)",
                // Calibration may store asymmetric thresholds; show the average half-range
                value = (settings.gazeRightThreshold - settings.gazeLeftThreshold) / 2f,
                range = 5f..30f,
                onValueChange = { viewModel.updateGazeSensitivity(it) },
                valueDisplay = { "${it.roundToInt()}°" }
            )

            ThresholdSlider(
                label = "Cooldown (ms)",
                value = settings.globalCooldownMs.toFloat(),
                range = 200f..2000f,
                onValueChange = { viewModel.updateCooldown(it.toLong()) },
                valueDisplay = { "${it.roundToInt()}ms" }
            )
        }
    }
}

@Composable
private fun CameraBox(
    hasCameraPermission: Boolean,
    faceDetected: Boolean,
    onBind: (Preview.SurfaceProvider) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!hasCameraPermission) {
            Text("Camera permission required", color = Color.White)
            return@Box
        }
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    onBind(surfaceProvider)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (faceDetected) Color(0xFF00E676) else Color(0xFFFF5252))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (faceDetected) "Face detected" else "No face", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun WizardCard(
    state: CalibrationState,
    canStart: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isRunning) {
                Text(
                    "Step ${state.stepNumber} of ${CalibrationStep.measured.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = state.step.instruction,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            if (state.isRunning) {
                Text(
                    if (state.isSettling) "Get ready…" else "Measuring…",
                    style = MaterialTheme.typography.bodySmall
                )
                LinearProgressIndicator(
                    progress = { if (state.isSettling) 0f else state.stepProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium)
            }
            if (state.step == CalibrationStep.DONE && state.before != null && state.after != null) {
                ResultSummary(state.before, state.after)
            }
            when {
                state.isRunning -> OutlinedButton(onClick = onCancel) { Text("Cancel") }
                state.error != null -> Button(onClick = onStart, enabled = canStart) { Text("Try Again") }
                state.step == CalibrationStep.DONE -> OutlinedButton(onClick = onStart, enabled = canStart) {
                    Text("Recalibrate")
                }
                else -> Button(onClick = onStart, enabled = canStart) { Text("Start Calibration") }
            }
        }
    }
}

@Composable
private fun ResultSummary(before: DetectionSettings, after: DetectionSettings) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SummaryRow("Blink threshold", "%.2f".format(before.blinkThreshold), "%.2f".format(after.blinkThreshold))
        SummaryRow("Turn left / right",
            "${before.gazeLeftThreshold.roundToInt()}° / ${before.gazeRightThreshold.roundToInt()}°",
            "${after.gazeLeftThreshold.roundToInt()}° / ${after.gazeRightThreshold.roundToInt()}°")
        SummaryRow("Tilt up / down",
            "${before.gazeDownThreshold.roundToInt()}° / ${before.gazeUpThreshold.roundToInt()}°",
            "${after.gazeDownThreshold.roundToInt()}° / ${after.gazeUpThreshold.roundToInt()}°")
    }
}

@Composable
private fun SummaryRow(label: String, before: String, after: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text("$before → $after", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ThresholdSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueDisplay: (Float) -> String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueDisplay(value), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
