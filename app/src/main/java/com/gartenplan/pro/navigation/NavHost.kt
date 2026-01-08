package com.gartenplan.pro.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gartenplan.pro.feature.calendar.CalendarScreen
import com.gartenplan.pro.feature.compost.CompostListScreen
import com.gartenplan.pro.feature.garden.GardenListScreen
import com.gartenplan.pro.feature.garden.canvas.GardenCanvasScreen
import com.gartenplan.pro.feature.garden.canvas.QuickGardenSetupScreen
import com.gartenplan.pro.feature.plants.PlantDetailScreen
import com.gartenplan.pro.feature.plants.PlantListScreen

/**
 * Main navigation graph for the app
 */
@Composable
fun GartenNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Garden.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        // ==================== GARDEN ====================
        
        // Garden List
        composable(route = Screen.Garden.route) {
            GardenListScreen(
                onGardenClick = { gardenId ->
                    navController.navigate(Routes.gardenCanvas(gardenId))
                },
                onCreateGarden = {
                    navController.navigate(Routes.GARDEN_SETUP)
                }
            )
        }

        // Quick Garden Setup (size presets)
        composable(route = Routes.GARDEN_SETUP) {
            QuickGardenSetupScreen(
                onGardenCreated = { name, widthCm, heightCm ->
                    navController.navigate(Routes.gardenCanvasNew(name, widthCm, heightCm)) {
                        popUpTo(Screen.Garden.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Garden Canvas (existing garden)
        composable(
            route = Routes.GARDEN_CANVAS,
            arguments = listOf(navArgument("gardenId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gardenId = backStackEntry.arguments?.getString("gardenId")
            GardenCanvasScreen(
                gardenId = gardenId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Garden Canvas (new garden)
        composable(
            route = Routes.GARDEN_CANVAS_NEW,
            arguments = listOf(
                navArgument("name") { 
                    type = NavType.StringType
                    defaultValue = "Mein Garten"
                },
                navArgument("width") { 
                    type = NavType.IntType
                    defaultValue = 500
                },
                navArgument("height") { 
                    type = NavType.IntType
                    defaultValue = 400
                }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Mein Garten"
            val width = backStackEntry.arguments?.getInt("width") ?: 500
            val height = backStackEntry.arguments?.getInt("height") ?: 400
            
            GardenCanvasScreen(
                gardenId = null,
                gardenName = name,
                gardenWidthCm = width,
                gardenHeightCm = height,
                onNavigateBack = { 
                    navController.navigate(Screen.Garden.route) {
                        popUpTo(Screen.Garden.route) { inclusive = true }
                    }
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
                }
            )
        }

        composable(
            route = Routes.TASK_DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            // TODO: TaskDetailScreen
        }

        // ==================== COMPOST ====================
        composable(route = Screen.Compost.route) {
            CompostListScreen(
                onCompostClick = { compostId ->
                    navController.navigate(Routes.compostDetail(compostId))
                },
                onCreateCompost = {
                    navController.navigate(Routes.COMPOST_CREATE)
                }
            )
        }

        composable(
            route = Routes.COMPOST_DETAIL,
            arguments = listOf(navArgument("compostId") { type = NavType.StringType })
        ) { backStackEntry ->
            // TODO: CompostDetailScreen
        }

        composable(route = Routes.COMPOST_CREATE) {
            // TODO: CreateCompostScreen
        }
    }
}
