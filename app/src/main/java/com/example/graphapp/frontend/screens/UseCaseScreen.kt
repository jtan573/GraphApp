package com.example.graphapp.frontend.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.graphapp.frontend.navigation.NavItem
import com.example.graphapp.frontend.viewmodels.GraphViewModel

@Composable
fun UseCaseScreen(
    navController: NavHostController,
) {
    val useCasesList = mapOf<String, () -> Unit>(
        "Threat Alert and Response" to {
            navController.navigate(NavItem.ThreatDetectionUseCase.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
            } },
        "Find Relevant Personnel" to {
            navController.navigate(NavItem.RelevantPersonnelUseCase.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
            }
        },
        "Route Integrity Check" to {
            navController.navigate(NavItem.RouteIntegrityUseCase.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
            } },
        "Suspicious Pattern Detection" to {
            navController.navigate(NavItem.SuspiciousActivityScreen.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
            } },
        "Shift Handover" to {
            navController.navigate(NavItem.ShiftHandoverScreen.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
            } },
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Use Cases",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        useCasesList.forEach { (title, onTap) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .clickable(onClick =  { onTap() }),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = title,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
