package com.gartenplan.pro.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gartenplan.pro.core.constants.TaskType
import com.gartenplan.pro.domain.model.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onTaskClick: (String) -> Unit,
    onNavigateToTaskList: (() -> Unit)? = null,
    onNavigateToSowingCalendar: (() -> Unit)? = null,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showGardenFilter by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kalender") },
                actions = {
                    // Garten-Filter
                    IconButton(onClick = { showGardenFilter = true }) {
                        Icon(Icons.Default.FilterList, "Filter")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.gardens.isEmpty()) {
            EmptyCalendarState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Monats-Navigation
                item {
                    MonthNavigation(
                        currentMonth = state.currentMonth,
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() }
                    )
                }

                // Kalender-Grid
                item {
                    CalendarGrid(
                        yearMonth = state.currentMonth,
                        selectedDate = state.selectedDate,
                        tasksByDay = state.monthTasks,
                        onDateSelected = { viewModel.selectDate(it) }
                    )
                }

                // Quick Actions
                item {
                    QuickActionRow(
                        onNavigateToTaskList = onNavigateToTaskList,
                        onNavigateToSowingCalendar = onNavigateToSowingCalendar
                    )
                }

                // Überfällige Aufgaben (wenn vorhanden)
                if (state.overdueTasks.isNotEmpty()) {
                    item {
                        OverdueTasksSection(
                            tasks = state.overdueTasks,
                            onToggleComplete = { viewModel.toggleTaskCompletion(it) },
                            getGardenName = { viewModel.getGardenName(it) }
                        )
                    }
                }

                // Aufgaben für ausgewählten Tag
                item {
                    SelectedDayTasksSection(
                        selectedDate = state.selectedDate,
                        tasks = state.selectedDayTasks,
                        onToggleComplete = { viewModel.toggleTaskCompletion(it) },
                        onDelete = { viewModel.deleteTask(it) },
                        getGardenName = { viewModel.getGardenName(it) }
                    )
                }

                // Platz am Ende
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Garten-Filter Dialog
    if (showGardenFilter) {
        GardenFilterDialog(
            gardens = state.gardens,
            selectedGardenId = state.selectedGardenId,
            onSelectGarden = {
                viewModel.selectGarden(it)
                showGardenFilter = false
            },
            onDismiss = { showGardenFilter = false }
        )
    }
}

@Composable
private fun QuickActionRow(
    onNavigateToTaskList: (() -> Unit)?,
    onNavigateToSowingCalendar: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onNavigateToTaskList != null) {
            OutlinedButton(
                onClick = onNavigateToTaskList,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Checklist, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Aufgabenliste")
            }
        }

        if (onNavigateToSowingCalendar != null) {
            OutlinedButton(
                onClick = onNavigateToSowingCalendar,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Grass, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Aussaat")
            }
        }
    }
}

@Composable
private fun EmptyCalendarState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Keine Aufgaben",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Erstelle einen Garten und plane deine Beete, um Aussaat- und Ernteaufgaben zu sehen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MonthNavigation(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Vorheriger Monat")
        }

        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Nächster Monat")
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    tasksByDay: Map<LocalDate, List<Task>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val daysOfWeek = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Wochentags-Header
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Kalender-Tage
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()

        // Berechne den Start (Montag der ersten Woche)
        val startOffset = (firstDayOfMonth.dayOfWeek.value - 1) // Mo=0, So=6
        val totalDays = lastDayOfMonth.dayOfMonth
        val totalCells = ((startOffset + totalDays + 6) / 7) * 7 // Aufrunden auf volle Wochen

        val days = (0 until totalCells).map { index ->
            val dayOffset = index - startOffset
            if (dayOffset in 0 until totalDays) {
                firstDayOfMonth.plusDays(dayOffset.toLong())
            } else null
        }

        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                week.forEach { day ->
                    CalendarDayCell(
                        date = day,
                        isSelected = day == selectedDate,
                        isToday = day == today,
                        tasks = day?.let { tasksByDay[it] } ?: emptyList(),
                        onClick = { day?.let(onDateSelected) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    tasks: List<Task>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSowing = tasks.any { it.isSowingTask() }
    val hasHarvest = tasks.any { it.isHarvestTask() }
    val hasOther = tasks.any { !it.isSowingTask() && !it.isHarvestTask() }
    val hasUncompletedTasks = tasks.any { !it.isCompleted }

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        date == null -> Color.Transparent
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(enabled = date != null, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (date != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Tag-Nummer
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                )

                // Task-Indikatoren
                if (tasks.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.height(8.dp)
                    ) {
                        if (hasSowing) {
                            TaskIndicatorDot(Color(0xFF4CAF50)) // Grün für Aussaat
                        }
                        if (hasHarvest) {
                            TaskIndicatorDot(Color(0xFFFF9800)) // Orange für Ernte
                        }
                        if (hasOther) {
                            TaskIndicatorDot(Color(0xFF2196F3)) // Blau für andere
                        }
                    }

                    // Anzahl unerledigter Aufgaben
                    val uncompletedCount = tasks.count { !it.isCompleted }
                    if (uncompletedCount > 0 && !isSelected) {
                        Text(
                            text = "$uncompletedCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskIndicatorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun OverdueTasksSection(
    tasks: List<Task>,
    onToggleComplete: (String) -> Unit,
    getGardenName: (String?) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "Überfällig (${tasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(8.dp))

            tasks.take(5).forEach { task ->
                TaskItem(
                    task = task,
                    gardenName = getGardenName(task.gardenId),
                    showDate = true,
                    isOverdue = true,
                    onToggleComplete = { onToggleComplete(task.id) },
                    onDelete = null
                )
            }

            if (tasks.size > 5) {
                Text(
                    "+ ${tasks.size - 5} weitere",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectedDayTasksSection(
    selectedDate: LocalDate,
    tasks: List<Task>,
    onToggleComplete: (String) -> Unit,
    onDelete: (String) -> Unit,
    getGardenName: (String?) -> String
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN)
    val isToday = selectedDate == LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = if (isToday) "Heute" else selectedDate.format(dateFormatter),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Keine Aufgaben an diesem Tag",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            tasks.forEach { task ->
                TaskItem(
                    task = task,
                    gardenName = getGardenName(task.gardenId),
                    showDate = false,
                    isOverdue = false,
                    onToggleComplete = { onToggleComplete(task.id) },
                    onDelete = { onDelete(task.id) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TaskItem(
    task: Task,
    gardenName: String,
    showDate: Boolean,
    isOverdue: Boolean,
    onToggleComplete: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val dateFormatter = DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = gardenName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (showDate) {
                        val dueDate = java.time.Instant.ofEpochMilli(task.dueDate)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        Text(
                            text = dueDate.format(dateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Priorität
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

            // Löschen-Button
            if (onDelete != null && !task.isAutoGenerated) {
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
private fun GardenFilterDialog(
    gardens: List<com.gartenplan.pro.domain.model.Garden>,
    selectedGardenId: String?,
    onSelectGarden: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Garten filtern") },
        text = {
            Column {
                // Alle Gärten Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectGarden(null) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGardenId == null,
                        onClick = { onSelectGarden(null) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Alle Gärten")
                }

                HorizontalDivider()

                // Einzelne Gärten
                gardens.forEach { garden ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectGarden(garden.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGardenId == garden.id,
                            onClick = { onSelectGarden(garden.id) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(garden.name)
                            Text(
                                "${garden.beds.size} Beete",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
