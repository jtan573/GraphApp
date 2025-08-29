package com.example.graphapp.core.services.kgraph

import com.example.graphapp.core.schema.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.core.schema.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.core.model.dto.EventDetailData
import com.example.graphapp.core.model.dto.EventDetails
import com.example.graphapp.core.services.kgraph.query.QueryService

interface KGraphService {

//    fun queryNaturalLanguage(query: String): Map<String, Float>

    /**
     * Retrieves events that are similar in multiple aspects (overall similarity).
     *
     * @param inputEventType Type of the input (source) event.
     * @param inputEventDetails Details of the input event to compare against.
     * @param targetEventType Optional target event type to filter results.
     * @param insightCategory Similarity dimension/category to use.
     * @return List of events with high similarity to the given event.
     */
    suspend fun findSimilarEvents(
        inputEventType: SchemaKeyEventTypeNames,
        inputEventDetails: EventDetailData,
        targetEventType: SchemaKeyEventTypeNames?,
        insightCategory: QueryService.InsightCategory
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
     * @param incidentInputMap Map of incident attributes and values.
     * @param taskInputMap Map of task attributes and values.
     * */
    suspend fun findThreatAlertAndResponse(
        incidentInputMap: Map<SchemaEventTypeNames, String>,
        taskInputMap: Map<SchemaEventTypeNames, String>,
    )

    /**
     * Retrieves incidents that are potentially suspicious based on location and behavioral similarity.
     * This function identifies past events that:
     * 1. Occurred near the specified event's location.
     * 2. Share similar "how" or "what" characteristics indicating a potentially suspicious pattern or approach.
     *
     * @param inputMap The input event containing descriptive and locational information.
     * @return A map grouping similar suspicious incidents by type or category, or `null` if no relevant incidents are found.
     */
    suspend fun findRelevantSuspiciousEvents(inputMap: Map<SchemaEventTypeNames, String>)

    /**
     * Retrieves information of a user's shift
     * @param userIdentifier Identifier of session user.
     * @return Map of timestamps to action.
     */
    fun findShiftHandoverSummary(userIdentifier: String): Map<Long, String>

}