package com.example.graphapp.data.api

import android.util.Log
import com.example.graphapp.backend.core.GraphSchema
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.SimilarEventTags
import com.example.graphapp.data.db.UserNodeEntity

// Function 3
data class DiscoverEventsResponse(
    val inputInformation: Map<SchemaEventTypeNames, String>,
    val predictedEvents: Map<GraphSchema.SchemaKeyEventTypeNames, List<EventDetails>>
)

data class EventDetails(
    val eventId: Long,
    val eventName: String,
    val eventProperties: Map<String, String>,
    val simScore: Float,
    val simProperties: List<SimilarEventTags>? = null
)

/* -------------------------------------------------
    Function 4: Pattern Recognition
------------------------------------------------- */
data class PatternFindingResponse(
    val similarNodes: List<List<KeyNode>>
)

data class KeyNode(
    val nodeName: String,
    val nodeDescription: String?,
    val nodeProperties: Map<String, String>
)

/* -------------------------------------------------
    Function 5: Detect Replica Event
------------------------------------------------- */
data class ReplicaDetectionResponse(
    val inputEvent: Map<String, String>,
    val topSimilarEvents: List<SimilarEvent>,
    val isLikelyDuplicate: Boolean
)

data class SimilarEvent(
    val eventName: String,
    val propertySimilarities: Map<String, Float>,
    val similarityRatio: Float,
    val averageSimilarityScore: Float
)

/* -------------------------------------------------
    Use Case 1: Contact relevant personnel
------------------------------------------------- */
data class ContactRelevantPersonnelResponse(
    val personnelMap: Map<UserNodeEntity, Int>?
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




