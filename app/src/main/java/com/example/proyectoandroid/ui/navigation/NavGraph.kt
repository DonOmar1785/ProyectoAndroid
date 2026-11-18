package com.example.proyectoandroid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.proyectoandroid.ui.screens.ARWorkspaceScreen
import com.example.proyectoandroid.ui.screens.CatalogScreen
import com.example.proyectoandroid.ui.screens.SplashScreen
import com.example.proyectoandroid.viewmodel.ARWorkspaceViewModel
import com.example.proyectoandroid.viewmodel.CatalogViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Catalog : Screen("catalog")
    object ARWorkspace : Screen("ar_workspace/{modelId}") {
        fun createRoute(modelId: String) = "ar_workspace/$modelId"
    }
}

@Composable
fun ARAssemblyNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val catalogViewModel: CatalogViewModel = viewModel()
    val arWorkspaceViewModel: ARWorkspaceViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToCatalog = {
                    navController.navigate(Screen.Catalog.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Catalog.route) {
            CatalogScreen(
                viewModel = catalogViewModel,
                onSelectModelForAR = { modelId ->
                    navController.navigate(Screen.ARWorkspace.createRoute(modelId))
                }
            )
        }

        composable(
            route = Screen.ARWorkspace.route,
            arguments = listOf(navArgument("modelId") { type = NavType.StringType })
        ) { backStackEntry ->
            val modelId = backStackEntry.arguments?.getString("modelId") ?: "model_taburete"
            ARWorkspaceScreen(
                modelId = modelId,
                viewModel = arWorkspaceViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
