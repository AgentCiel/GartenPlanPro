package com.gartenplan.pro.feature.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBedScreen(
    gardenId: String,
    onNavigateBack: () -> Unit,
    onBedCreated: (String) -> Unit,
    viewModel: CreateBedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(gardenId) {
        viewModel.setGardenId(gardenId)
    }

    LaunchedEffect(uiState) {
        if (uiState is CreateBedUiState.Success) {
            onBedCreated((uiState as CreateBedUiState.Success).bedId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neues Beet") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.createBed() },
                        enabled = uiState !is CreateBedUiState.Loading
                    ) {
                        Icon(Icons.Default.Check, "Speichern")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text("Name des Beets") },
                placeholder = { Text("z.B. Tomaten-Beet") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = viewModel.nameError != null,
                supportingText = viewModel.nameError?.let { { Text(it) } }
            )

            // Größe
            Text("Größe", style = MaterialTheme.typography.titleMedium)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.widthCm,
                    onValueChange = { viewModel.widthCm = it.filter { c -> c.isDigit() } },
                    label = { Text("Breite (cm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = viewModel.sizeError != null
                )
                OutlinedTextField(
                    value = viewModel.heightCm,
                    onValueChange = { viewModel.heightCm = it.filter { c -> c.isDigit() } },
                    label = { Text("Länge (cm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = viewModel.sizeError != null
                )
            }
            viewModel.sizeError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Position
            Text("Position im Garten", style = MaterialTheme.typography.titleMedium)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.positionX,
                    onValueChange = { viewModel.positionX = it.filter { c -> c.isDigit() } },
                    label = { Text("X (cm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = viewModel.positionY,
                    onValueChange = { viewModel.positionY = it.filter { c -> c.isDigit() } },
                    label = { Text("Y (cm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Farbe
            Text("Farbe", style = MaterialTheme.typography.titleMedium)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bedColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(color)))
                            .border(
                                width = if (viewModel.colorHex == color) 3.dp else 1.dp,
                                color = if (viewModel.colorHex == color) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { viewModel.colorHex = color }
                    )
                }
            }

            // Error
            if (uiState is CreateBedUiState.Error) {
                Text(
                    text = (uiState as CreateBedUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private val bedColors = listOf(
    "#8D6E63", // Brown (default)
    "#4CAF50", // Green
    "#FF9800", // Orange
    "#2196F3", // Blue
    "#9C27B0", // Purple
    "#F44336", // Red
    "#607D8B", // Blue Grey
    "#795548"  // Dark Brown
)
