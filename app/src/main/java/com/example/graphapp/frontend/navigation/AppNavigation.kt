package com.example.graphapp.frontend.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens.AlertNearbyTroopersScreen
import com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens.AssignTaskToTroopersScreen
import com.example.graphapp.frontend.useCaseScreens.routeIntegrityScreens.ReRouteIntegrityAnnotatedMapScreen
import com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens.ReceivedTaskScreen
import com.example.graphapp.frontend.useCaseScreens.routeIntegrityScreens.RouteIntegrityAnnotatedMapScreen
import com.example.graphapp.frontend.useCaseScreens.suspiciousBehaviourScreens.SuspiciousActivityByLocationScreen
import com.example.graphapp.frontend.screens.GraphViewScreen
import com.example.graphapp.frontend.screens.PersonnelScreen
import com.example.graphapp.frontend.screens.UseCaseScreen
import com.example.graphapp.frontend.useCaseScreens.relevantPersonnelScreens.RelevantPersonnelMainScreen
import com.example.graphapp.frontend.useCaseScreens.routeIntegrityScreens.RouteIntegrityMainScreen
import com.example.graphapp.frontend.useCaseScreens.suspiciousBehaviourScreens.SuspiciousBehaviourUseCaseScreen
import com.example.graphapp.frontend.useCaseScreens.suspiciousBehaviourScreens.SuspiciousEventsDetailsScreen
import com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens.TaskInstructionsScreen
import com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens.ThreatAnalysisScreen
import com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens.ThreatDetectionAnalysisScreen
import com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens.ThreatDetectionMainScreen
import com.example.graphapp.frontend.viewmodels.GraphViewModel

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
        composable(NavItem.RelevantPersonnelUseCase.route) {
            RelevantPersonnelMainScreen(viewModel = viewModel, navController = navController)
        }
        composable(NavItem.ThreatDetectionUseCase.route) {
            ThreatDetectionMainScreen(navController = navController)
        }
        composable(NavItem.SuspiciousPatternUseCase.route) {
            SuspiciousBehaviourUseCaseScreen(viewModel = viewModel, navController = navController)
        }
        composable(NavItem.RouteIntegrityUseCase.route) {
            RouteIntegrityMainScreen(navController = navController)
        }

        // Demo Purposes
        composable(NavItem.RouteIntegrityImage.route) {
            RouteIntegrityAnnotatedMapScreen(navController)
        }
        composable(NavItem.ReRouteIntegrityImage.route) {
            ReRouteIntegrityAnnotatedMapScreen(navController)
        }
        composable(NavItem.AlertTroopersScreen.route) {
            AlertNearbyTroopersScreen()
        }
        composable(NavItem.AssignTaskScreen.route) {
            AssignTaskToTroopersScreen(navController = navController)
        }
        composable(NavItem.ReceivedTaskScreen.route) {
            ReceivedTaskScreen(navController = navController)
        }
        composable(NavItem.TaskInstructionsScreen.route) {
            TaskInstructionsScreen()
        }
        composable(NavItem.ThreatAnalysisScreen.route) {
            ThreatAnalysisScreen(viewModel = viewModel)
        }
        composable(NavItem.SuspiciousLocationScreen.route) {
            SuspiciousActivityByLocationScreen(navController = navController)
        }
        composable(NavItem.SuspiciousEventsDetailScreen.route) {
            SuspiciousEventsDetailsScreen(viewModel = viewModel)
        }
    }
}