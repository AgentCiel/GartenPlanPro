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
import com.gartenplan.pro.feature.garden.CreateBedScreen
import com.gartenplan.pro.feature.garden.CreateGardenScreen
import com.gartenplan.pro.feature.garden.GardenDetailScreen
import com.gartenplan.pro.feature.garden.GardenListScreen
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
        composable(route = Screen.Garden.route) {
            GardenListScreen(
                onGardenClick = { gardenId ->
                    navController.navigate(Routes.gardenDetail(gardenId))
                },
                onCreateGarden = {
                    navController.navigate(Routes.GARDEN_CREATE)
                }
            )
        }

        composable(route = Routes.GARDEN_CREATE) {
            CreateGardenScreen(
                onNavigateBack = { navController.popBackStack() },
                onGardenCreated = { gardenId ->
                    navController.navigate(Routes.gardenDetail(gardenId)) {
                        popUpTo(Screen.Garden.route)
                    }
                }
            )
        }

        composable(
            route = Routes.GARDEN_DETAIL,
            arguments = listOf(navArgument("gardenId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gardenId = backStackEntry.arguments?.getString("gardenId") ?: return@composable
            GardenDetailScreen(
                gardenId = gardenId,
                onNavigateBack = { navController.popBackStack() },
                onBedClick = { bedId ->
                    navController.navigate(Routes.bedDetail(gardenId, bedId))
                },
                onAddBed = {
                    navController.navigate(Routes.createBed(gardenId))
                }
            )
        }

        composable(
            route = Routes.CREATE_BED,
            arguments = listOf(navArgument("gardenId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gardenId = backStackEntry.arguments?.getString("gardenId") ?: return@composable
            CreateBedScreen(
                gardenId = gardenId,
                onNavigateBack = { navController.popBackStack() },
                onBedCreated = { navController.popBackStack() }
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