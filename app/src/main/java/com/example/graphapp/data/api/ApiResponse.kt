package com.example.graphapp.data.api

import android.util.Log
import com.example.graphapp.backend.dto.GraphSchema
import com.example.graphapp.backend.dto.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.schema.SimilarEventTags
import com.example.graphapp.data.db.UserNodeEntity

enum class ResponseStatus {
    SUCCESS, ERROR, FAILED
}

data class ApiResponse(
    val status: ResponseStatus,
    val timestamp: Long,
    val message: String? = null,
    val data: ResponseData?
)

sealed class ResponseData {
    data class PredictMissingPropertiesData(val payload: PredictMissingPropertiesResponse) : ResponseData()
    data class ProvideRecommendationsData(val payload: ProvideRecommendationsResponse) : ResponseData()
    data class PatternFindingData(val payload: PatternFindingResponse) : ResponseData()
    data class DiscoverEventsData(val payload: DiscoverEventsResponse) : ResponseData()
    data class DetectReplicaEventData(val payload: ReplicaDetectionResponse) : ResponseData()
    data class ContactPersonnelData(val payload: ContactRelevantPersonnelResponse) : ResponseData()
    data class ThreatAlertData(val payload: ThreatAlertResponse) : ResponseData()
}

/* -------------------------------------------------
    Function 1: Predict missing properties
------------------------------------------------- */
data class PredictMissingPropertiesResponse(
    val predictions: List<PredictMissingProperties>
)

data class PredictMissingProperties(
    val nodeId: Long,
    val nodeDetails: NodeDetails,
    val predictedProperties: List<PredictedProperty>
)

data class NodeDetails(
    val type: String,
    val name: String,
    val description: String?,
    val existingProperties: Map<String, String>
)

data class PredictedProperty(
    val propertyType: String,
    val propertyValue: String,
    val simScore: Float,
    val mostSimilarKeyNode: String
)

/* -------------------------------------------------
    Function 2: Provide event recommendations on input event
    Function 3: Provide event recommendations on input property
------------------------------------------------- */
//sealed class EventRecommendationResult {
//    data class EventToEventRec(val items: ProvideRecommendationsResponse) : EventRecommendationResult()
//    data class PropertyToEventRec(val items: DiscoverEventsResponse) : EventRecommendationResult()
//}

data class ProvideRecommendationsResponse(
    val inputEvent: Map<String, String>,
    val recommendations: Map<String, List<String>>
)

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

/* -------------------------------------------------
    Helper Function: Build ApiResponse
------------------------------------------------- */
fun buildApiResponseFromResult(result: Any): ApiResponse {
    val responseData = when (result) {
        is PredictMissingPropertiesResponse -> ResponseData.PredictMissingPropertiesData(result)
        is ProvideRecommendationsResponse -> ResponseData.ProvideRecommendationsData(result)
        is PatternFindingResponse -> ResponseData.PatternFindingData(result)
        is DiscoverEventsResponse -> ResponseData.DiscoverEventsData(result)
        is ReplicaDetectionResponse -> ResponseData.DetectReplicaEventData(result)

        // Use cases
        is ContactRelevantPersonnelResponse -> ResponseData.ContactPersonnelData(result)
        is ThreatAlertResponse -> ResponseData.ThreatAlertData(result)

        else -> throw IllegalArgumentException("Unsupported response type: ${result::class}")
    }

    val apiRes = ApiResponse(
        status = ResponseStatus.SUCCESS,
        timestamp = System.currentTimeMillis(),
        data = responseData
    )
    Log.d("API RESPONSE", "Response: $apiRes")

    return apiRes
}



