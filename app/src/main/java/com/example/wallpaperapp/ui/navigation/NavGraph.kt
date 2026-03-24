package com.example.wallpaperapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.wallpaperapp.data.db.AppDatabase
import com.example.wallpaperapp.data.repository.HabitRepository
import com.example.wallpaperapp.ui.addedit.AddEditHabitScreen
import com.example.wallpaperapp.ui.addedit.AddEditHabitViewModel
import com.example.wallpaperapp.ui.home.HomeScreen
import com.example.wallpaperapp.ui.home.HomeViewModel
import com.example.wallpaperapp.ui.preview.WallpaperPreviewScreen
import com.example.wallpaperapp.ui.preview.WallpaperPreviewViewModel
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
data class AddEditHabitRoute(val habitId: Long = -1L)

@Serializable
object WallpaperPreviewRoute

@Composable
fun DotStreakNavGraph(database: AppDatabase) {
    val navController = rememberNavController()
    val repository = HabitRepository(database)

    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(repository))
            HomeScreen(
                viewModel = vm,
                repository = repository,
                onAddHabit = { navController.navigate(AddEditHabitRoute()) },
                onEditHabit = { id -> navController.navigate(AddEditHabitRoute(habitId = id)) },
                onOpenPreview = { navController.navigate(WallpaperPreviewRoute) }
            )
        }
        composable<AddEditHabitRoute> { backStackEntry ->
            val route: AddEditHabitRoute = backStackEntry.toRoute()
            val vm: AddEditHabitViewModel = viewModel(
                factory = AddEditHabitViewModel.factory(repository, route.habitId)
            )
            AddEditHabitScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<WallpaperPreviewRoute> {
            val vm: WallpaperPreviewViewModel = viewModel(
                factory = WallpaperPreviewViewModel.factory(repository)
            )
            WallpaperPreviewScreen(
                viewModel = vm,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
