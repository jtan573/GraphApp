package com.example.graphapp.data.api

import android.util.Log
import com.example.graphapp.backend.core.GraphSchema
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.SimilarEventTags
import com.example.graphapp.data.db.UserNodeEntity

data class EventDetails(
    val eventId: Long,
    val eventName: String,
    val eventProperties: Map<String, String>,
    val simScore: Float,
    val simProperties: List<SimilarEventTags>? = null
)

/* -------------------------------------------------
    Use Case 2/3/4: Threat Alert
------------------------------------------------- */
data class ThreatAlertResponse(
    val nearbyActiveUsersMap: Map<UserNodeEntity, Int>? = null,
    val potentialImpacts: Map<Long, List<String>>? = null,
    val potentialTasks: List<EventDetails>? = null,
    val taskAssignment: Map<String, List<UserNodeEntity>>? = null,
    val similarIncidents: List<EventDetails>? = null,
    val incidentsAffectingStations: Map<DisruptionCause, List<EventDetails>>? = null
)

/* -------------------------------------------------
    Use Case 4: Route Integrity
------------------------------------------------- */
enum class DisruptionCause {
    PROXIMITY, WIND
}




