package com.example.graphapp.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.graphapp.ui.components.BottomNavBar
import com.example.graphapp.ui.navigation.AppNavigation
import com.example.graphapp.ui.viewmodels.GraphViewModel

@Composable
fun MainScreen(viewModel: GraphViewModel, navController: NavHostController) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = { BottomNavBar(navController = navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            AppNavigation(navController = navController, viewModel = viewModel)
        }
    }
}
