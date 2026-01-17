package com.gartenplan.pro.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gartenplan.pro.domain.model.Plant
import com.gartenplan.pro.feature.calendar.CalendarScreen
import com.gartenplan.pro.feature.calendar.TaskListScreen
import com.gartenplan.pro.feature.calendar.SowingCalendarScreen
import com.gartenplan.pro.feature.moon.MoonScreen
import com.gartenplan.pro.feature.garden.GardenListScreen
import com.gartenplan.pro.feature.garden.CreateGardenScreen
import com.gartenplan.pro.feature.garden.editor.GardenEditorScreen
import com.gartenplan.pro.feature.garden.editor.BedDetailScreen
import com.gartenplan.pro.feature.garden.editor.PlantPickerDialog
import com.gartenplan.pro.feature.plants.PlantDetailScreen
import com.gartenplan.pro.feature.plants.PlantListScreen

@Composable
fun GartenNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // State für Plant Picker Dialog
    var showPlantPicker by remember { mutableStateOf(false) }
    var plantPickerBedId by remember { mutableStateOf<String?>(null) }
    var availablePlants by remember { mutableStateOf<List<Plant>>(emptyList()) }

    NavHost(
        navController = navController,
        startDestination = Screen.Garden.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        // ==================== GARDEN ====================
        
        // 5.1 Garden Overview
        composable(route = Screen.Garden.route) {
            GardenListScreen(
                onGardenClick = { gardenId ->
                    navController.navigate(Routes.gardenEditor(gardenId))
                },
                onCreateGarden = {
                    navController.navigate(Routes.GARDEN_CREATE)
                }
            )
        }

        // Create Garden
        composable(route = Routes.GARDEN_CREATE) {
            CreateGardenScreen(
                onGardenCreated = { name, widthM, heightM ->
                    navController.navigate(Routes.gardenEditorNew(name, widthM, heightM)) {
                        popUpTo(Screen.Garden.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5.2 Garden Editor (existing garden)
        composable(
            route = Routes.GARDEN_EDITOR,
            arguments = listOf(navArgument("gardenId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gardenId = backStackEntry.arguments?.getString("gardenId")
            GardenEditorScreen(
                gardenId = gardenId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBedDetail = { bedId ->
                    navController.navigate(Routes.bedDetail(bedId))
                }
            )
        }

        // 5.2 Garden Editor (new garden)
        composable(
            route = Routes.GARDEN_EDITOR_NEW,
            arguments = listOf(
                navArgument("name") { 
                    type = NavType.StringType
                    defaultValue = "Mein Garten"
                },
                navArgument("width") { 
                    type = NavType.FloatType
                    defaultValue = 5f
                },
                navArgument("height") { 
                    type = NavType.FloatType
                    defaultValue = 4f
                }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Mein Garten"
            val width = backStackEntry.arguments?.getFloat("width") ?: 5f
            val height = backStackEntry.arguments?.getFloat("height") ?: 4f
            
            GardenEditorScreen(
                gardenId = null,
                gardenName = name,
                gardenWidthM = width,
                gardenHeightM = height,
                onNavigateBack = { 
                    navController.navigate(Screen.Garden.route) {
                        popUpTo(Screen.Garden.route) { inclusive = true }
                    }
                },
                onNavigateToBedDetail = { bedId ->
                    navController.navigate(Routes.bedDetail(bedId))
                }
            )
        }

        // 5.3 Bed Detail
        composable(
            route = Routes.BED_DETAIL,
            arguments = listOf(navArgument("bedId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bedId = backStackEntry.arguments?.getString("bedId") ?: return@composable
            
            BedDetailScreen(
                bedId = bedId,
                onNavigateBack = { navController.popBackStack() },
                onOpenPlantPicker = {
                    plantPickerBedId = bedId
                    showPlantPicker = true
                }
            )
        }

        // ==================== PLANTS ====================
        
        composable(route = Screen.Plants.route) {
            PlantListScreen(
                onPlantClick = { plantId ->
                    navController.navigate(Routes.plantDetail(plantId))
                }
            )
        }

        composable(
            route = Routes.PLANT_DETAIL,
            arguments = listOf(navArgument("plantId") { type = NavType.StringType })
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId") ?: return@composable
            PlantDetailScreen(
                plantId = plantId,
                onBack = { navController.popBackStack() }
            )
        }

        // ==================== CALENDAR ====================

        composable(route = Screen.Calendar.route) {
            CalendarScreen(
                onTaskClick = { taskId ->
                    navController.navigate(Routes.taskDetail(taskId))
                },
                onNavigateToTaskList = {
                    navController.navigate(Routes.TASK_LIST)
                },
                onNavigateToSowingCalendar = {
                    navController.navigate(Routes.SOWING_CALENDAR)
                }
            )
        }

        // Task List Screen
        composable(route = Routes.TASK_LIST) {
            TaskListScreen(
                onNavigateBack = { navController.popBackStack() },
                onTaskClick = { taskId ->
                    navController.navigate(Routes.taskDetail(taskId))
                }
            )
        }

        // Sowing Calendar Screen
        composable(route = Routes.SOWING_CALENDAR) {
            SowingCalendarScreen(
                onNavigateBack = { navController.popBackStack() },
                onPlantClick = { plantId ->
                    navController.navigate(Routes.plantDetail(plantId))
                }
            )
        }

        // ==================== MOON ====================

        composable(route = Screen.Moon.route) {
            MoonScreen()
        }
    }

    // 5.4 Plant Picker Dialog
    if (showPlantPicker) {
        PlantPickerDialog(
            plants = availablePlants,
            onPlantSelected = { plant ->
                // TODO: Add plant to bed
                showPlantPicker = false
            },
            onDismiss = { showPlantPicker = false }
        )
    }
}
