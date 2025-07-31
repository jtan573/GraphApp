package com.example.graphapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavItem(val route: String, val label: String, val icon: ImageVector?) {

    // Main Components
    object Graph : NavItem("graph", "Graph", Icons.Default.Share)
    object Personnel : NavItem("personnel", "Personnel", Icons.Default.Person)
    object UseCase : NavItem("useCase", "UseCase", Icons.Default.Build)

    // Use Cases
    object ThreatDetectionUseCase : NavItem("threatDetection", "ThreatDetection", null)
    object SuspiciousPatternUseCase : NavItem("suspiciousPattern", "SuspiciousPattern", null)
    object RouteIntegrityUseCase : NavItem("routeIntegrity", "RouteIntegrity", null)

    // Demo Purposes
    object RouteIntegrityImage : NavItem("routeIntegrityImage", "RouteIntegrityImage", null)
    object ReRouteIntegrityImage : NavItem("reRouteIntegrityImage", "ReRouteIntegrityImage", null)
    object PersonnelMapImage : NavItem("personnelMapImage", "PersonnelMapImage", null)
    object AlertTroopersScreen : NavItem("alertTroopersScreen", "AlertTroopersScreen", null)
    object AssignTaskScreen : NavItem("assignTaskScreen", "AssignTaskScreen", null)
    object ReceivedTaskScreen : NavItem("receivedTaskScreen", "ReceivedTaskScreen", null)
    object SuspiciousLocationScreen : NavItem("suspiciousLocationScreen", "SuspiciousLocationScreen", null)

    companion object {
        val items = listOf(Personnel, Graph, UseCase)
    }
}