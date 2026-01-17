package com.gartenplan.pro.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom Navigation Destinations
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Garden : Screen(
        route = "garden",
        title = "Garten",
        selectedIcon = Icons.Filled.Yard,
        unselectedIcon = Icons.Outlined.Yard
    )
    
    data object Plants : Screen(
        route = "plants",
        title = "Pflanzen",
        selectedIcon = Icons.Filled.Eco,
        unselectedIcon = Icons.Outlined.Eco
    )
    
    data object Calendar : Screen(
        route = "calendar",
        title = "Kalender",
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    )
    
    data object Compost : Screen(
        route = "compost",
        title = "Kompost",
        selectedIcon = Icons.Filled.Recycling,
        unselectedIcon = Icons.Outlined.Recycling
    )
    
    companion object {
        val bottomNavItems = listOf(Garden, Plants, Calendar, Compost)
    }
}

/**
 * All Routes
 */
object Routes {
    // Garden
    const val GARDEN_LIST = "garden"
    const val GARDEN_CREATE = "garden/create"
    const val GARDEN_EDITOR = "garden/editor/{gardenId}"
    const val GARDEN_EDITOR_NEW = "garden/editor/new?name={name}&width={width}&height={height}"
    const val BED_DETAIL = "garden/bed/{bedId}"
    
    // Plants
    const val PLANT_LIST = "plants"
    const val PLANT_DETAIL = "plant/{plantId}"
    
    // Calendar
    const val CALENDAR = "calendar"
    const val TASK_LIST = "calendar/tasks"
    const val SOWING_CALENDAR = "calendar/sowing"
    const val TASK_DETAIL = "task/{taskId}"
    
    // Compost
    const val COMPOST_LIST = "compost"
    const val COMPOST_DETAIL = "compost/{compostId}"
    
    // Helper Functions
    fun gardenEditor(gardenId: String) = "garden/editor/$gardenId"
    fun gardenEditorNew(name: String, widthM: Float, heightM: Float) = 
        "garden/editor/new?name=$name&width=$widthM&height=$heightM"
    fun bedDetail(bedId: String) = "garden/bed/$bedId"
    fun plantDetail(plantId: String) = "plant/$plantId"
    fun taskDetail(taskId: String) = "task/$taskId"
    fun compostDetail(compostId: String) = "compost/$compostId"
}
