package com.gartenplan.pro.feature.garden

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGardenScreen(
    onNavigateBack: () -> Unit,
    onGardenCreated: (String) -> Unit,
    viewModel: CreateGardenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is CreateGardenUiState.Success) {
            onGardenCreated((uiState as CreateGardenUiState.Success).gardenId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neuer Garten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.createGarden() },
                        enabled = uiState !is CreateGardenUiState.Loading
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
                label = { Text("Name des Gartens") },
                placeholder = { Text("z.B. Gemüsegarten Hinterhof") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = viewModel.nameError != null,
                supportingText = viewModel.nameError?.let { { Text(it) } }
            )

            // Größe
            Text(
                text = "Größe",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.widthMeter,
                    onValueChange = { viewModel.widthMeter = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Breite (m)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = viewModel.sizeError != null
                )
                OutlinedTextField(
                    value = viewModel.heightMeter,
                    onValueChange = { viewModel.heightMeter = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Länge (m)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = viewModel.sizeError != null
                )
            }
            viewModel.sizeError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Fläche Anzeige
            val area = try {
                val w = viewModel.widthMeter.toFloatOrNull() ?: 0f
                val h = viewModel.heightMeter.toFloatOrNull() ?: 0f
                w * h
            } catch (e: Exception) { 0f }
            
            if (area > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Fläche: %.1f m²".format(area),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Klimazone
            Text(
                text = "Klimazone (optional)",
                style = MaterialTheme.typography.titleMedium
            )
            
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = viewModel.climateZone,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Klimazone") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    climateZones.forEach { zone ->
                        DropdownMenuItem(
                            text = { Text(zone) },
                            onClick = {
                                viewModel.climateZone = zone
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Notizen
            OutlinedTextField(
                value = viewModel.notes,
                onValueChange = { viewModel.notes = it },
                label = { Text("Notizen (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // Error
            if (uiState is CreateGardenUiState.Error) {
                Text(
                    text = (uiState as CreateGardenUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Spacer für FAB
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private val climateZones = listOf(
    "5a (-28.9 bis -26.1°C)",
    "5b (-26.1 bis -23.3°C)",
    "6a (-23.3 bis -20.6°C)",
    "6b (-20.6 bis -17.8°C)",
    "7a (-17.8 bis -15.0°C)",
    "7b (-15.0 bis -12.2°C)",
    "8a (-12.2 bis -9.4°C)",
    "8b (-9.4 bis -6.7°C)"
)
