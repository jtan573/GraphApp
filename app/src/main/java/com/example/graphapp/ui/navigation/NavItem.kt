package com.example.graphapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavItem(val route: String, val label: String, val icon: ImageVector?) {
    object Graph : NavItem("graph", "Graph", Icons.Default.Share)
    object Events : NavItem("events", "Events", Icons.Default.Star)
    object Personnel : NavItem("personnel", "Personnel", Icons.Default.Person)
    object UseCase : NavItem("useCase", "UseCase", Icons.Default.Build)

    object ThreatDetectionUseCase : NavItem("threatDetection", "ThreatDetection", null)

    companion object {
        val items = listOf(Personnel, Graph, Events, UseCase)
    }
}