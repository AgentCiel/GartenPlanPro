package com.gartenplan.pro.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for bottom navigation
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
 * Sub-routes for navigation within features
 */
object Routes {
    // Garden - Canvas-based
    const val GARDEN_LIST = "garden"
    const val GARDEN_SETUP = "garden/setup"
    const val GARDEN_CANVAS = "garden/canvas/{gardenId}"
    const val GARDEN_CANVAS_NEW = "garden/canvas/new?name={name}&width={width}&height={height}"
    
    // Plants
    const val PLANT_LIST = "plants"
    const val PLANT_DETAIL = "plant/{plantId}"
    const val PLANT_SEARCH = "plants/search"
    
    // Calendar
    const val CALENDAR_OVERVIEW = "calendar"
    const val TASK_DETAIL = "task/{taskId}"
    const val TASK_CREATE = "task/create"
    
    // Compost
    const val COMPOST_LIST = "compost"
    const val COMPOST_DETAIL = "compost/{compostId}"
    const val COMPOST_CREATE = "compost/create"
    
    // Settings
    const val SETTINGS = "settings"
    
    // Helper functions
    fun gardenCanvas(gardenId: String) = "garden/canvas/$gardenId"
    fun gardenCanvasNew(name: String, widthCm: Int, heightCm: Int) = 
        "garden/canvas/new?name=$name&width=$widthCm&height=$heightCm"
    fun plantDetail(plantId: String) = "plant/$plantId"
    fun taskDetail(taskId: String) = "task/$taskId"
    fun compostDetail(compostId: String) = "compost/$compostId"
}
