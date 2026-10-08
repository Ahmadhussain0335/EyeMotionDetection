@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.app.eyesmotiondetection.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader("Appearance")
            ToggleRow("Dark Mode", isDark) { viewModel.setDarkMode(it) }

            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader("Feedback")
            ToggleRow("Haptic Feedback", settings.isVibrationEnabled) { viewModel.setVibration(it) }
            ToggleRow("Voice Feedback", settings.isVoiceFeedbackEnabled) { viewModel.setVoiceFeedback(it) }

            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader("Interface")
            ToggleRow("Show Overlay Indicator", settings.isOverlayEnabled) { viewModel.setOverlay(it) }
            ToggleRow("Log Gesture History", settings.isLoggingEnabled) { viewModel.setLogging(it) }

            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader("Face Pointer Control")
            ToggleRow("Face Pointer (Mouse Mode)", settings.isFacePointerEnabled) {
                viewModel.setFacePointerEnabled(it)
            }
            AnimatedVisibility(visible = settings.isFacePointerEnabled) {
                Column {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            "Pointer Speed: ${"%.1f".format(settings.pointerSpeed)}x",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = settings.pointerSpeed,
                            onValueChange = { viewModel.setPointerSpeed(it) },
                            valueRange = 0.5f..4.0f
                        )
                    }
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            "Sensitivity: ${"%.1f".format(settings.pointerSensitivity)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = settings.pointerSensitivity,
                            onValueChange = { viewModel.setPointerSensitivity(it) },
                            valueRange = 1.0f..8.0f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader("Detection")

            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Fast Blink Count Threshold: ${settings.fastBlinkCountThreshold}",
                    style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.fastBlinkCountThreshold.toFloat(),
                    onValueChange = {
                        viewModel.updateSettings(settings.copy(fastBlinkCountThreshold = it.toInt()))
                    },
                    valueRange = 2f..6f, steps = 3
                )
            }

            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Global Cooldown: ${settings.globalCooldownMs}ms",
                    style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.globalCooldownMs.toFloat(),
                    onValueChange = {
                        viewModel.updateSettings(settings.copy(globalCooldownMs = it.toLong()))
                    },
                    valueRange = 200f..2000f
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
    HorizontalDivider()
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = value, onCheckedChange = onToggle)
    }
}
