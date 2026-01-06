package com.gartenplan.pro.feature.compost

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gartenplan.pro.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompostListScreen(
    onCompostClick: (String) -> Unit,
    onCreateCompost: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kompost") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateCompost) {
                Icon(Icons.Default.Add, "Kompost anlegen")
            }
        }
    ) { padding ->
        // TODO: Implement compost list
        EmptyState(
            icon = Icons.Default.Recycling,
            title = "Noch kein Kompost",
            message = "Lege einen Kompost an und tracke das Grün/Braun-Verhältnis für optimale Ergebnisse.",
            actionLabel = "Kompost anlegen",
            onAction = onCreateCompost,
            modifier = Modifier.padding(padding)
        )
    }
}
