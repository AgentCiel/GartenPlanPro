package com.gartenplan.pro.feature.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gartenplan.pro.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onTaskClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kalender") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        // TODO: Implement calendar with tasks
        EmptyState(
            icon = Icons.Default.CalendarMonth,
            title = "Keine Aufgaben",
            message = "Erstelle einen Garten und füge Pflanzen hinzu, um Aussaat- und Ernteaufgaben zu erhalten.",
            modifier = Modifier.padding(padding)
        )
    }
}
