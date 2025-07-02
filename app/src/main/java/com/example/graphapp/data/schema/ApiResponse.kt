package com.example.graphapp.data.schema

data class ApiResponse(
    val status: String,
    val timestamp: String,
    val data: ResponseData
)

sealed class ResponseData {
    data class PredictMissingPropertiesData(val payload: PredictMissingPropertiesResponse) : ResponseData()
    data class ProvideRecommendationsData(val payload: ProvideRecommendationsResponse) : ResponseData()
    data class PatternFindingData(val payload: PatternFindingResponse) : ResponseData()
    data class PredictingData(val payload: DiscoverEventsResponse) : ResponseData()
}

/*
Function 1: Predict Missing Properties
Predicted Properties:  mutableMapOf<Long, List<Pair<String, Long>>>()
*/
data class PredictMissingPropertiesResponse(
    val predictions: List<PredictMissingProperties>
)

data class PredictMissingProperties(
    val nodeId: Long,
    val nodeDetails: NodeDetails,
    val predictedProperties: Map<String, String>
)

data class NodeDetails(
    val type: String,
    val name: String,
    val existingProperties: Map<String, String>
)

// Function 2: Provide recommendations to an input task
data class ProvideRecommendationsResponse(
    val inputTask: Map<String, String>,
    val recommendations: List<Recommendation>
)

data class Recommendation(
    val recType: String,
    val recItems: List<String>,
//    val confidence: Double
)

// Function 3: Pattern Finding
data class PatternFindingResponse(
    val similarNodes: List<List<KeyNode>>
)

data class KeyNode(
    val nodeName: String,
    val nodeProperties: Map<String, String>
)

// Function 4:  Discover
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
    val eventProperties: Map<String, String>
)
