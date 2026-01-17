package com.gartenplan.pro.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.core.constants.TaskType
import com.gartenplan.pro.domain.model.Garden
import com.gartenplan.pro.domain.model.Task
import com.gartenplan.pro.domain.usecase.garden.GetAllGardensUseCase
import com.gartenplan.pro.domain.usecase.task.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val gardens: List<Garden> = emptyList(),
    val selectedGardenId: String? = null, // null = alle Gärten
    val monthTasks: Map<LocalDate, List<Task>> = emptyMap(),
    val selectedDayTasks: List<Task> = emptyList(),
    val overdueTasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val showTaskDialog: Boolean = false,
    val editingTask: Task? = null
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getAllGardensUseCase: GetAllGardensUseCase,
    private val getTasksForDateRangeUseCase: GetTasksForDateRangeUseCase,
    private val getTasksForDateUseCase: GetTasksForDateUseCase,
    private val getOverdueTasksUseCase: GetOverdueTasksUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        loadGardens()
    }

    private fun loadGardens() {
        viewModelScope.launch {
            getAllGardensUseCase().collect { gardens ->
                _state.value = _state.value.copy(
                    gardens = gardens,
                    isLoading = false
                )
                // Nach Laden der Gärten: Tasks laden
                loadTasksForMonth()
                loadOverdueTasks()
            }
        }
    }

    fun selectMonth(yearMonth: YearMonth) {
        _state.value = _state.value.copy(currentMonth = yearMonth)
        loadTasksForMonth()
    }

    fun nextMonth() {
        selectMonth(_state.value.currentMonth.plusMonths(1))
    }

    fun previousMonth() {
        selectMonth(_state.value.currentMonth.minusMonths(1))
    }

    fun selectDate(date: LocalDate) {
        _state.value = _state.value.copy(selectedDate = date)
        loadTasksForSelectedDay()
    }

    fun selectGarden(gardenId: String?) {
        _state.value = _state.value.copy(selectedGardenId = gardenId)
        loadTasksForMonth()
        loadTasksForSelectedDay()
        loadOverdueTasks()
    }

    private fun loadTasksForMonth() {
        val state = _state.value
        val month = state.currentMonth
        val startDate = month.atDay(1)
        val endDate = month.atEndOfMonth()

        viewModelScope.launch {
            val allTasks = mutableListOf<Task>()
            val gardens = if (state.selectedGardenId != null) {
                state.gardens.filter { it.id == state.selectedGardenId }
            } else {
                state.gardens
            }

            // Sammle Tasks von allen relevanten Gärten
            gardens.forEach { garden ->
                getTasksForDateRangeUseCase(garden.id, startDate, endDate)
                    .first()
                    .let { allTasks.addAll(it) }
            }

            // Gruppiere nach Tag
            val tasksByDay = allTasks.groupBy { task ->
                java.time.Instant.ofEpochMilli(task.dueDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }

            _state.value = _state.value.copy(monthTasks = tasksByDay)
            loadTasksForSelectedDay()
        }
    }

    private fun loadTasksForSelectedDay() {
        val state = _state.value
        val selectedDate = state.selectedDate

        viewModelScope.launch {
            val allTasks = mutableListOf<Task>()
            val gardens = if (state.selectedGardenId != null) {
                state.gardens.filter { it.id == state.selectedGardenId }
            } else {
                state.gardens
            }

            gardens.forEach { garden ->
                getTasksForDateUseCase(garden.id, selectedDate)
                    .first()
                    .let { allTasks.addAll(it) }
            }

            // Sortiere: Unerledigte zuerst, dann nach Priorität
            val sorted = allTasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.priority }
                    .thenBy { it.dueDate }
            )

            _state.value = _state.value.copy(selectedDayTasks = sorted)
        }
    }

    private fun loadOverdueTasks() {
        val state = _state.value

        viewModelScope.launch {
            val allOverdue = mutableListOf<Task>()
            val gardens = if (state.selectedGardenId != null) {
                state.gardens.filter { it.id == state.selectedGardenId }
            } else {
                state.gardens
            }

            gardens.forEach { garden ->
                getOverdueTasksUseCase(garden.id)
                    .first()
                    .let { allOverdue.addAll(it) }
            }

            val sorted = allOverdue.sortedBy { it.dueDate }
            _state.value = _state.value.copy(overdueTasks = sorted)
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            toggleTaskCompletionUseCase(taskId)
            // Refresh nach Änderung
            loadTasksForMonth()
            loadOverdueTasks()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            deleteTaskUseCase(taskId)
            loadTasksForMonth()
            loadOverdueTasks()
        }
    }

    fun getGardenName(gardenId: String?): String {
        return _state.value.gardens.find { it.id == gardenId }?.name ?: "Unbekannt"
    }
}

// Helper für Task-Anzeige
fun Task.getIcon(): String = when (taskType) {
    TaskType.SOW_INDOOR -> "🌱"
    TaskType.SOW_OUTDOOR -> "🌰"
    TaskType.TRANSPLANT -> "🌿"
    TaskType.HARVEST -> "🧺"
    TaskType.WATER -> "💧"
    TaskType.FERTILIZE -> "🧪"
    TaskType.PRUNE -> "✂️"
    TaskType.WEED -> "🌾"
    TaskType.PEST_CONTROL -> "🐛"
    TaskType.COMPOST -> "♻️"
    TaskType.OTHER -> "📋"
}

fun Task.isSowingTask(): Boolean = taskType in listOf(
    TaskType.SOW_INDOOR, TaskType.SOW_OUTDOOR, TaskType.TRANSPLANT
)

fun Task.isHarvestTask(): Boolean = taskType == TaskType.HARVEST
