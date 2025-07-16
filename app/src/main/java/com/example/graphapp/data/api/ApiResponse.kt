package com.example.graphapp.data.api

data class ApiResponse(
    val status: String,
    val timestamp: String,
    val data: ResponseData
)

sealed class ResponseData {
    data class PredictMissingPropertiesData(val payload: PredictMissingPropertiesResponse) : ResponseData()
    data class ProvideRecommendationsData(val payload: ProvideRecommendationsResponse) : ResponseData()
    data class PatternFindingData(val payload: PatternFindingResponse) : ResponseData()
    data class DiscoverEventsData(val payload: DiscoverEventsResponse) : ResponseData()
    data class DetectReplicaEventData(val payload: ReplicaDetectionResponse) : ResponseData()
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
    val recommendations: List<Recommendation>
)

data class Recommendation(
    val recType: String,
    val recItems: List<String>
)

// Function 3
data class DiscoverEventsResponse(
    val inputInformation: Map<String, String>,
    val predictedEvents: List<PredictedEventByType>
)

data class PredictedEventByType(
    val eventType: String,
    val eventList: List<EventDetails>,
)

data class EventDetails(
    val eventName: String,
    val eventProperties: Map<String, String>,
    val simScore: Float
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



