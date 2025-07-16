package com.example.graphapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.graphapp.ui.screens.EventScreen
import com.example.graphapp.ui.screens.GraphViewScreen
import com.example.graphapp.ui.screens.PersonnelScreen
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
        composable(NavItem.Graph.route) {
            GraphViewScreen(viewModel = viewModel, navController = navController)
        }
        composable(NavItem.Events.route) {
            EventScreen(viewModel = viewModel)
        }
        composable(NavItem.Personnel.route) {
            PersonnelScreen(viewModel = viewModel)
        }
    }
}