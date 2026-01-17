package com.gartenplan.pro.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gartenplan.pro.domain.model.Bed
import com.gartenplan.pro.domain.model.Garden
import com.gartenplan.pro.domain.model.Task
import com.gartenplan.pro.domain.usecase.garden.GetAllGardensUseCase
import com.gartenplan.pro.domain.usecase.task.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListState(
    val allTasks: List<Task> = emptyList(),
    val overdueTasks: List<Task> = emptyList(),
    val upcomingTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val gardens: List<Garden> = emptyList(),
    val availableBeds: List<Bed> = emptyList(),
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val selectedBedId: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val getAllGardensUseCase: GetAllGardensUseCase,
    private val getTasksByGardenUseCase: GetTasksByGardenUseCase,
    private val getOverdueTasksUseCase: GetOverdueTasksUseCase,
    private val getUpcomingTasksUseCase: GetUpcomingTasksUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TaskListState())
    val state: StateFlow<TaskListState> = _state.asStateFlow()

    init {
        loadGardens()
    }

    private fun loadGardens() {
        viewModelScope.launch {
            getAllGardensUseCase().collect { gardens ->
                val allBeds = gardens.flatMap { it.beds }
                _state.value = _state.value.copy(
                    gardens = gardens,
                    availableBeds = allBeds,
                    isLoading = false
                )
                loadAllTasks()
            }
        }
    }

    private fun loadAllTasks() {
        viewModelScope.launch {
            val gardens = _state.value.gardens
            val bedFilter = _state.value.selectedBedId

            val allTasks = mutableListOf<Task>()
            val overdueTasks = mutableListOf<Task>()
            val upcomingTasks = mutableListOf<Task>()

            gardens.forEach { garden ->
                // Alle Tasks
                getTasksByGardenUseCase(garden.id).first().let { tasks ->
                    val filtered = if (bedFilter != null) {
                        tasks.filter { it.bedId == bedFilter }
                    } else tasks
                    allTasks.addAll(filtered)
                }

                // Überfällige
                getOverdueTasksUseCase(garden.id).first().let { tasks ->
                    val filtered = if (bedFilter != null) {
                        tasks.filter { it.bedId == bedFilter }
                    } else tasks
                    overdueTasks.addAll(filtered)
                }

                // Anstehende (14 Tage)
                getUpcomingTasksUseCase(garden.id, 14).first().let { tasks ->
                    val filtered = if (bedFilter != null) {
                        tasks.filter { it.bedId == bedFilter }
                    } else tasks
                    upcomingTasks.addAll(filtered)
                }
            }

            // Sortieren
            val sortedAll = allTasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.dueDate }
                    .thenByDescending { it.priority }
            )

            val completedTasks = allTasks.filter { it.isCompleted }
                .sortedByDescending { it.completedAt ?: it.dueDate }

            _state.value = _state.value.copy(
                allTasks = sortedAll.filter { !it.isCompleted },
                overdueTasks = overdueTasks.sortedBy { it.dueDate },
                upcomingTasks = upcomingTasks.filter { !it.isCompleted }.sortedBy { it.dueDate },
                completedTasks = completedTasks
            )
        }
    }

    fun setFilter(filter: TaskFilter) {
        _state.value = _state.value.copy(selectedFilter = filter)
    }

    fun setBedFilter(bedId: String?) {
        _state.value = _state.value.copy(selectedBedId = bedId)
        loadAllTasks()
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            toggleTaskCompletionUseCase(taskId)
            loadAllTasks()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            deleteTaskUseCase(taskId)
            loadAllTasks()
        }
    }

    fun getGardenName(gardenId: String?): String {
        return _state.value.gardens.find { it.id == gardenId }?.name ?: "Unbekannt"
    }
}
