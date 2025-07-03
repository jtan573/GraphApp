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
    val mostSimilarKeyNode: String
)

// Function 2: Provide recommendations to an input task
sealed class EventRecommendationResult {
    data class EventToEventRec(val items: List<Recommendation>) : EventRecommendationResult()
    data class PropertyToEventRec(val items: List<PredictedEventByType>) : EventRecommendationResult()
}

data class ProvideRecommendationsResponse(
    val inputEvent: Map<String, String>,
    val recommendations: List<Recommendation>
)

data class Recommendation(
    val recType: String,
    val recItems: List<String>
)

// Function 3: Discover events
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

// Function 4:  Pattern Finding
data class PatternFindingResponse(
    val similarNodes: List<List<KeyNode>>
)
data class KeyNode(
    val nodeName: String,
    val nodeDescription: String?,
    val nodeProperties: Map<String, String>
)

/* -------------------------------------------------
    Function 5: Anomaly Detection
------------------------------------------------- */
data class AnomalyDetectionResponse(
    val inputEvent: Map<String, String>,
    val overallAnomaly: Boolean,
    val flaggedPropertyCount: Int,
    val propertyAnalyses: List<PropertyAgreement>,
    val topSimilarEvents: List<SimilarEvent>
)

data class PropertyAgreement(
    val propertyType: String,
    val candidateValue: String, // From new event
    val isAnomalous: Boolean
)

data class SimilarEvent(
    val eventName: String,
    val propertyValues: Map<String, String>
)


/* API Response
* For Predict Missing Properties Function
ApiResponse(
    status=success,
    timestamp=,
    data=PredictMissingPropertiesData(
        payload=PredictMissingPropertiesResponse(
            predictions=[
                PredictMissingProperties(
                    nodeId=31,
                    nodeDetails=NodeDetails(
                        type=Article,
                        name=Cyber Attack,
                        description=null,
                        existingProperties={Entity=Group Delta, Method=Malware, Date=2021-05-30, Motive=Data Theft}),
                        predictedProperties=[
                            PredictedProperty(
                                propertyType=Location,
                                propertyValue=Government Servers,
                                mostSimilarKeyNode=Cyber Breach
                            )
                        ]
                    ),
            ]
        )
    )
)


*
*  For Recommend Event on Event Input Function
ApiResponse(
    status=success,
    timestamp=,
    data=ProvideRecommendationsData(
        payload=ProvideRecommendationsResponse(
            inputTask={Method=Truck, Article=CarAccident},
            recommendations=[
                Recommendation(recType=Article, recItems=[Truck Ramming, Vehicle Attack]),
                Recommendation(recType=Entity, recItems=[Group Gamma, Group Alpha]),
                Recommendation(recType=Method, recItems=[Explosives, Suicide Vest]),
                Recommendation(recType=Location, recItems=[City Square, Market District]),
                Recommendation(recType=Motive, recItems=[Maximize Casualties, Intimidation])
            ]
        )
    )
)

* For recommend event on Property Input
ApiResponse(
    status=success,
    timestamp=,
    data=DiscoverEventsData(
        payload=DiscoverEventsResponse(
            inputInformation={Method=Truck},
            predictedEvents=[
                PredictedEventByType(
                    eventType=Article,
                    eventList=[
                        EventDetails(eventName=Bombing, eventProperties={Entity=Group Alpha, Method=Explosives, Location=Market District, Motive=Intimidation}),
                        EventDetails(eventName=Suicide Bombing, eventProperties={Entity=Sect Zeta, Method=Suicide Vest, Location=Train Station, Motive=Religious Motivation})
                    ]
                )
            ]
        )
    )
)

* Finding patterns
ApiResponse(
    status=success,
    timestamp=,
    data=PatternFindingData(
        payload=PatternFindingResponse(
            similarNodes=[
                [
                    KeyNode(nodeName=Bombing, nodeProperties={Entity=Group Alpha, Method=Explosives, Location=Market District, Motive=Intimidation}),
                    KeyNode(nodeName=Explosion, nodeProperties={Method=Explosives, Location=Market District, Motive=Intimidation})
                ],
                [
                    KeyNode(nodeName=Vehicle Attack, nodeProperties={Entity=Group Gamma, Method=Truck, Location=City Square, Motive=Maximize Casualties}),
                    KeyNode(nodeName=Truck Ramming, nodeProperties={Entity=Group Gamma, Method=Truck, Location=City Square})
                ]
            ]
        )
    )
)
*/



