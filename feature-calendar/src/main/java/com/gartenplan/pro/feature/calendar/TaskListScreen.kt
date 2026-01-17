package com.gartenplan.pro.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gartenplan.pro.domain.model.Bed
import com.gartenplan.pro.domain.model.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

enum class TaskFilter {
    ALL, OVERDUE, UPCOMING, COMPLETED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onNavigateBack: () -> Unit,
    onTaskClick: (String) -> Unit,
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aufgaben") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück")
                    }
                },
                actions = {
                    // Filter
                    IconButton(onClick = { showFilterDialog = true }) {
                        BadgedBox(
                            badge = {
                                if (state.selectedBedId != null) {
                                    Badge { }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FilterList, "Filter")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab-Leiste
            TaskFilterTabs(
                selectedFilter = state.selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) },
                overdueCount = state.overdueTasks.size,
                upcomingCount = state.upcomingTasks.size
            )

            // Task-Liste
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val tasksToShow = when (state.selectedFilter) {
                    TaskFilter.ALL -> state.allTasks
                    TaskFilter.OVERDUE -> state.overdueTasks
                    TaskFilter.UPCOMING -> state.upcomingTasks
                    TaskFilter.COMPLETED -> state.completedTasks
                }

                if (tasksToShow.isEmpty()) {
                    EmptyTaskState(
                        filter = state.selectedFilter,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    TaskList(
                        tasks = tasksToShow,
                        onToggleComplete = { viewModel.toggleTaskCompletion(it) },
                        onDelete = { viewModel.deleteTask(it) },
                        getGardenName = { viewModel.getGardenName(it) }
                    )
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        BedFilterDialog(
            beds = state.availableBeds,
            selectedBedId = state.selectedBedId,
            onSelectBed = {
                viewModel.setBedFilter(it)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
private fun TaskFilterTabs(
    selectedFilter: TaskFilter,
    onFilterSelected: (TaskFilter) -> Unit,
    overdueCount: Int,
    upcomingCount: Int
) {
    ScrollableTabRow(
        selectedTabIndex = selectedFilter.ordinal,
        edgePadding = 16.dp
    ) {
        TaskFilter.entries.forEach { filter ->
            val label = when (filter) {
                TaskFilter.ALL -> "Alle"
                TaskFilter.OVERDUE -> if (overdueCount > 0) "Überfällig ($overdueCount)" else "Überfällig"
                TaskFilter.UPCOMING -> if (upcomingCount > 0) "Anstehend ($upcomingCount)" else "Anstehend"
                TaskFilter.COMPLETED -> "Erledigt"
            }

            Tab(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                text = {
                    Text(
                        text = label,
                        color = if (filter == TaskFilter.OVERDUE && overdueCount > 0)
                            MaterialTheme.colorScheme.error
                        else
                            LocalContentColor.current
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyTaskState(
    filter: TaskFilter,
    modifier: Modifier = Modifier
) {
    val (icon, title, message) = when (filter) {
        TaskFilter.ALL -> Triple(
            Icons.Default.CheckCircle,
            "Keine Aufgaben",
            "Erstelle einen Garten und plane deine Pflanzen, um Aufgaben zu erhalten."
        )
        TaskFilter.OVERDUE -> Triple(
            Icons.Default.CheckCircle,
            "Alles erledigt",
            "Keine überfälligen Aufgaben."
        )
        TaskFilter.UPCOMING -> Triple(
            Icons.Default.EventAvailable,
            "Keine anstehenden Aufgaben",
            "In den nächsten 14 Tagen stehen keine Aufgaben an."
        )
        TaskFilter.COMPLETED -> Triple(
            Icons.Default.History,
            "Noch nichts erledigt",
            "Erledigte Aufgaben erscheinen hier."
        )
    }

    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TaskList(
    tasks: List<Task>,
    onToggleComplete: (String) -> Unit,
    onDelete: (String) -> Unit,
    getGardenName: (String?) -> String
) {
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)

    // Gruppiere nach Datum
    val groupedTasks = tasks.groupBy { task ->
        Instant.ofEpochMilli(task.dueDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }.toSortedMap()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedTasks.forEach { (date, dayTasks) ->
            // Datum-Header
            item(key = "header-$date") {
                val isOverdue = date.isBefore(today)
                val isToday = date == today

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isToday -> "Heute"
                            date == today.plusDays(1) -> "Morgen"
                            else -> date.format(dateFormatter)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverdue) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurface
                    )

                    if (isOverdue) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Warning,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Tasks für diesen Tag
            items(dayTasks, key = { it.id }) { task ->
                TaskListItem(
                    task = task,
                    gardenName = getGardenName(task.gardenId),
                    isOverdue = Instant.ofEpochMilli(task.dueDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .isBefore(today) && !task.isCompleted,
                    onToggleComplete = { onToggleComplete(task.id) },
                    onDelete = { onDelete(task.id) }
                )
            }
        }

        // Platz für FAB
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun TaskListItem(
    task: Task,
    gardenName: String,
    isOverdue: Boolean,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )

            Spacer(Modifier.width(8.dp))

            // Icon
            Text(
                text = task.getIcon(),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.width(12.dp))

            // Inhalt
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                task.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = gardenName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (task.priority == 3) {
                        Icon(
                            Icons.Default.PriorityHigh,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Löschen-Button (nur für manuelle Tasks)
            if (!task.isAutoGenerated) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        "Löschen",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BedFilterDialog(
    beds: List<Bed>,
    selectedBedId: String?,
    onSelectBed: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nach Beet filtern") },
        text = {
            Column {
                // Alle Beete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectBed(null) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedBedId == null,
                        onClick = { onSelectBed(null) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Alle Beete")
                }

                if (beds.isNotEmpty()) {
                    HorizontalDivider()

                    beds.forEach { bed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectBed(bed.id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedBedId == bed.id,
                                onClick = { onSelectBed(bed.id) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(bed.name.ifEmpty { "Unbenanntes Beet" })
                        }
                    }
                } else {
                    Text(
                        "Keine Beete vorhanden",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}
