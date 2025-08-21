package com.example.graphapp.frontend.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavItem(val route: String, val label: String, val icon: ImageVector?) {

    // Main Components
    object Graph : NavItem("graph", "Graph", Icons.Default.Share)
    object Personnel : NavItem("personnel", "Personnel", Icons.Default.Person)
    object UseCase : NavItem("useCase", "UseCase", Icons.Default.Build)

    // Use Cases
    object RelevantPersonnelUseCase : NavItem("relevantPersonnel", "RelevantPersonnel", null)

    object RouteIntegrityUseCase : NavItem("routeIntegrity", "RouteIntegrity", null)
    object RouteIntegrityImage : NavItem("routeIntegrityImage", "RouteIntegrityImage", null)
    object ReRouteIntegrityImage : NavItem("reRouteIntegrityImage", "ReRouteIntegrityImage", null)

    object ThreatDetectionUseCase : NavItem("threatDetection", "ThreatDetection", null)
    object AlertTroopersScreen : NavItem("alertTroopersScreen", "AlertTroopersScreen", null)
    object ReceivedTaskScreen : NavItem("receivedTaskScreen", "ReceivedTaskScreen", null)
    object TaskInstructionsScreen : NavItem("taskInstructionsScreen", "TaskInstructionsScreen", null)
    object ThreatAnalysisScreen : NavItem("threatAnalysisScreen", "ThreatAnalysisScreen", null)

    object SuspiciousActivityScreen : NavItem("suspiciousActivityScreen", "SuspiciousActivityScreen", null)
    object SuspiciousEventsDetailScreen : NavItem("suspiciousDetailsScreen", "SuspiciousDetailsScreen", null)

    companion object {
        val items = listOf(Personnel, Graph, UseCase)
    }
}