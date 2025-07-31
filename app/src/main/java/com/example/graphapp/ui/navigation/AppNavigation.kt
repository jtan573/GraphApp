package com.example.graphapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.graphapp.ui.imageScreens.AlertNearbyTroopersScreen
import com.example.graphapp.ui.imageScreens.AssignTaskToTroopersScreen
import com.example.graphapp.ui.imageScreens.ContactPersonnelMapScreen
import com.example.graphapp.ui.imageScreens.ReRouteIntegrityAnnotatedMapScreen
import com.example.graphapp.ui.imageScreens.ReceivedTaskScreen
import com.example.graphapp.ui.imageScreens.RouteIntegrityAnnotatedMapScreen
import com.example.graphapp.ui.imageScreens.SuspiciousActivityByLocationScreen
import com.example.graphapp.ui.screens.GraphViewScreen
import com.example.graphapp.ui.screens.PersonnelScreen
import com.example.graphapp.ui.screens.UseCaseScreen
import com.example.graphapp.ui.useCaseScreens.RouteIntegrityUseCaseScreen
import com.example.graphapp.ui.useCaseScreens.SuspiciousBehaviourUseCaseScreen
import com.example.graphapp.ui.useCaseScreens.ThreatDetectionUseCaseScreen
import com.example.graphapp.ui.viewmodels.GraphViewModel

@Composable
fun AppNavigation (
    navController: NavHostController,
    viewModel: GraphViewModel
) {
    NavHost(
        navController = navController,
        startDestination = NavItem.Graph.route
    ) {
        composable(NavItem.Personnel.route) {
            PersonnelScreen(viewModel = viewModel, navController = navController)
        }
        composable(NavItem.Graph.route) {
            GraphViewScreen(viewModel = viewModel, navController = navController)
        }
        composable(NavItem.UseCase.route) {
            UseCaseScreen(viewModel = viewModel, navController = navController)
        }
        composable(NavItem.ThreatDetectionUseCase.route) {
            ThreatDetectionUseCaseScreen(viewModel = viewModel, navController = navController)
        }
        composable(NavItem.SuspiciousPatternUseCase.route) {
            SuspiciousBehaviourUseCaseScreen(viewModel = viewModel, navController = navController)
        }
        composable(NavItem.RouteIntegrityUseCase.route) {
            RouteIntegrityUseCaseScreen(viewModel = viewModel, navController = navController)
        }

        // Demo Purposes
        composable(NavItem.PersonnelMapImage.route) {
            ContactPersonnelMapScreen()
        }
        composable(NavItem.RouteIntegrityImage.route) {
            RouteIntegrityAnnotatedMapScreen(navController)
        }
        composable(NavItem.ReRouteIntegrityImage.route) {
            ReRouteIntegrityAnnotatedMapScreen(navController)
        }
        composable(NavItem.AlertTroopersScreen.route) {
            AlertNearbyTroopersScreen(navController)
        }
        composable(NavItem.AssignTaskScreen.route) {
            AssignTaskToTroopersScreen(navController = navController)
        }
        composable(NavItem.ReceivedTaskScreen.route) {
            ReceivedTaskScreen()
        }
        composable(NavItem.SuspiciousLocationScreen.route) {
            SuspiciousActivityByLocationScreen()
        }
    }
}