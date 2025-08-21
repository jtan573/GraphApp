package com.example.graphapp.backend.services.kgraph

import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.backend.services.kgraph.query.QueryService
import com.example.graphapp.data.api.EventDetailData
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.ThreatAlertResponse

interface KGraphService {

//    fun getInsight(category: InsightService.InsightCategory, contextId: String? = null): String
//
//    fun getInsights(categories: List<InsightService.InsightCategory>, contextId: String? = null): Map<InsightService.InsightCategory, String>
//
//    fun discoverRelationships(contextId: String? = null): List<String>

//    fun queryNaturalLanguage(query: String): Map<String, Float>

    /**
     * Retrieves events that are similar in multiple aspects (overall similarity).
     * @param givenEventType Type of event.
     * @param eventDetails 5W1H of event.
     * @param targetEventType Type of event user wants to retrieve.
     * @return List of events with high similarity to the given event.
     */
    suspend fun findSimilarEvents(
        givenEventType: SchemaKeyEventTypeNames,
        targetEventType: SchemaKeyEventTypeNames?,
        eventDetails: EventDetailData,
    ): Map<SchemaKeyEventTypeNames, List<EventDetails>>

    /**
     * Retrieves events that are similar in a specific aspect (overall similarity).
     * @param givenEventType Type of event.
     * @param givenPropertyValue Value of property to calculate similarity against.
     * @param targetEventType Type of event user wants to retrieve.
     * @param targetSimilarityProperty Property to compute similarity on.
     * @return List of events with high similarity to the given event.
     */
    suspend fun findSimilarEventsByProperty(
        inputEventType: SchemaKeyEventTypeNames?,
        targetSimilarityProperty: QueryService.InsightCategory?,
        inputPropertyValue: String,
        targetEventType: SchemaKeyEventTypeNames?
    ): Map<SchemaKeyEventTypeNames, List<EventDetails>>

    /**
     * Retrieves personnel who are within a 3km radius (configurable) from the specified location
     * and have a relevant specialisation based on the input description.
     *
     * @param inputLocation String representing coordinates of current location.
     * @param inputDescription String representing description of target personnel.
     * @return Map containing a list of matched personnel with relevant specialisations
     * and distance away from the specified location.
     */
    suspend fun findRelevantPersonnelOnDemand(inputLocation: String, inputDescription: String)

    /**
     * Checks the integrity of a given route by identifying nearby incidents.
     *
     * This function retrieves:
     * 1. Incidents located within a 3km radius of any provided coordinate.
     * 2. Airborne incidents that may impact the route if any part of the route falls within the
     *      incident's projected path, determined by its wind direction and a ±30° bearing allowance (configurable).
     *
     * @param routeCoordinates A list of coordinates (as strings) representing the route to evaluate.
     * @return A map grouping relevant incidents by type or category, or `null` if no relevant incidents are found.
     */
    suspend fun findAffectedRouteStationsByLocation(routeCoordinates: List<String>)

    /**
     * Analyzes the given event and generates a threat alert response that includes situational awareness,
     * resource allocation, and related incident insights.
     *
     * @param eventInput The details of the incoming event, including what, how, and where components.
     * @return A [ThreatAlertResponse] containing context-specific recommendations and data to support response planning.
     *      1. Identify nearby active users and their distance away from incident.
     *      2. Suggest potential impacts based on the nature and context of the event.
     *      3. Suggest potential response tasks based on prior event-task patterns.
     *      4. Recommend personnel assignments for each task based on specialisation and proximity.
     *      5. Retrieve similar historical incidents for reference or comparison.
     */
    suspend fun findThreatAlertAndResponse(inputMap: Map<String, String>)

    /**
     * Retrieves incidents that are potentially suspicious based on location and behavioral similarity.
     * This function identifies past events that:
     * 1. Occurred near the specified event's location.
     * 2. Share similar "how" or "what" characteristics indicating a potentially suspicious pattern or approach.
     *
     * @param event The input event containing descriptive and locational information.
     * @return A map grouping similar suspicious incidents by type or category, or `null` if no relevant incidents are found.
     */
    suspend fun findSimilarSuspiciousEventsByLocationAndApproach(inputMap: Map<String, String>)

}