package com.example.graphapp.backend.model.dto

import com.example.graphapp.backend.core.GraphSchema.DisruptionCause
import com.example.graphapp.data.db.UserNodeEntity

data class ThreatAlertResponse(
    val nearbyActiveUsersMap: Map<UserNodeEntity, Int>? = null,
    val potentialImpacts: Map<Long, List<String>>? = null,
    val potentialTasks: List<EventDetails>? = null,
    val taskAssignment: Map<String, List<UserNodeEntity>>? = null,
    val similarIncidents: List<EventDetails>? = null,
    val incidentsAffectingStations: Map<DisruptionCause, List<EventDetails>>? = null
)