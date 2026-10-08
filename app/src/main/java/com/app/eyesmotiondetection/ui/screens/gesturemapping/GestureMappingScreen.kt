@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.app.eyesmotiondetection.ui.screens.gesturemapping

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.eyesmotiondetection.domain.model.AppAction
import com.app.eyesmotiondetection.domain.model.EyeGesture
import com.app.eyesmotiondetection.domain.model.GestureMapping

@Composable
fun GestureMappingScreen(
    onBack: () -> Unit,
    viewModel: GestureMappingViewModel = hiltViewModel()
) {
    val mappings by viewModel.mappings.collectAsState()
    val mappingMap = mappings.associateBy { it.gesture }

    var showResetDialog by remember { mutableStateOf(false) }
    var editingGesture by remember { mutableStateOf<EyeGesture?>(null) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Mappings") },
            text = { Text("Reset all gesture mappings to defaults?") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetToDefaults(); showResetDialog = false }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    editingGesture?.let { gesture ->
        ActionPickerDialog(
            gesture = gesture,
            current = mappingMap[gesture]?.action ?: AppAction.NONE,
            onSelect = { action ->
                viewModel.updateMapping(gesture, action)
                editingGesture = null
            },
            onDismiss = { editingGesture = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gesture Mapping") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(EyeGesture.supported) { gesture ->
                val mapping = mappingMap[gesture]
                GestureRow(
                    gesture = gesture,
                    mapping = mapping,
                    onEditClick = { editingGesture = gesture },
                    onToggle = { enabled -> viewModel.toggleMapping(gesture, enabled) }
                )
            }
        }
    }
}

@Composable
private fun GestureRow(
    gesture: EyeGesture,
    mapping: GestureMapping?,
    onEditClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(gesture.displayName, fontWeight = FontWeight.Medium)
                Text(
                    mapping?.action?.displayName ?: "Not mapped",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (mapping != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (mapping != null) {
                Switch(
                    checked = mapping.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            TextButton(onClick = onEditClick) {
                Text(if (mapping != null) "Change" else "Assign")
            }
        }
    }
}

@Composable
private fun ActionPickerDialog(
    gesture: EyeGesture,
    current: AppAction,
    onSelect: (AppAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Action for ${gesture.displayName}") },
        text = {
            LazyColumn {
                items(AppAction.entries) { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = action == current,
                            onClick = { onSelect(action) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(action.displayName)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
