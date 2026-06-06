package com.trainseat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.trainseat.app.presentation.addedit.AddEditAlertScreen
import com.trainseat.app.presentation.dashboard.DashboardScreen
import com.trainseat.app.presentation.detail.AlertDetailScreen
import com.trainseat.app.presentation.settings.SettingsScreen
import com.trainseat.app.presentation.theme.TrainSeatAlertTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrainSeatAlertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {

        composable("dashboard") {
            DashboardScreen(
                onAddAlert = { navController.navigate("add_edit/0") },
                onEditAlert = { id -> navController.navigate("add_edit/$id") },
                onViewDetail = { id -> navController.navigate("detail/$id") },
                onSettings = { navController.navigate("settings") }
            )
        }

        composable(
            route = "add_edit/{alertId}",
            arguments = listOf(navArgument("alertId") {
                type = NavType.LongType
                defaultValue = 0L
            })
        ) {
            AddEditAlertScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = "detail/{alertId}",
            arguments = listOf(navArgument("alertId") {
                type = NavType.LongType
            }),
            deepLinks = listOf(navDeepLink { uriPattern = "trainseat://alert/{alertId}" })
        ) {
            AlertDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditAlert = { id -> navController.navigate("add_edit/$id") }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
