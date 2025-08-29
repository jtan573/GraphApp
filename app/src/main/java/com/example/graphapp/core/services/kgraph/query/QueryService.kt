package com.example.graphapp.core.services.kgraph.query

import com.example.graphapp.core.schema.GraphSchema.DisruptionCause
import com.example.graphapp.core.schema.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.core.model.dto.EventDetailData
import com.example.graphapp.core.model.dto.EventDetails
import com.example.graphapp.core.model.dto.ThreatAlertResponse
import com.example.graphapp.data.db.UserNodeEntity

interface QueryService {

    enum class InsightCategory {
        WHO, WHEN, WHERE, WHY, HOW, SUSPICIOUS, ALERT
    }

    suspend fun ensureReady()

    /**
     * FUTURE EXTENSION
     */
    fun queryNaturalLanguage(query: String): Map<String, Float>

    /**
     * Retrieves events that are similar in multiple aspects (overall similarity).
     * @param eventType Type of event.
     * @param eventDetails 5W1H of event.
     * @param targetEventType Type of event user wants to retrieve.
     * @return List of events with high similarity to the given event.
     */
    suspend fun querySimilarEvents(
        eventType: SchemaKeyEventTypeNames,
        eventDetails: EventDetailData,
        targetEventType: SchemaKeyEventTypeNames?,
        insightCategory: InsightCategory?,
    ): Map<SchemaKeyEventTypeNames, List<EventDetails>>

    /**
     * Retrieves personnel who are within a 3km radius (configurable) from the specified location
     * and have a relevant specialisation based on the input description.
     *
     * @param inputLoc String representing coordinates of current location.
     * @param inputDesc String representing description of target personnel.
     * @return Map containing a list of matched personnel with relevant specialisations
     * and distance away from the specified location.
     */
    suspend fun findRelevantPersonnel(inputLoc: String, inputDesc: String): Map<UserNodeEntity, Int>?

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
    suspend fun checkRouteIntegrity(routeCoordinates: List<String>): Map<DisruptionCause, List<EventDetails>>?

    /**
     * Analyzes the given event and generates a threat alert response that includes situational awareness,
     * resource allocation, and related incident insights.
     *
     * @param incidentEventInput Map of incident attributes and values.
     * @param taskEventInput Map of task attributes and values.
     * @return A [ThreatAlertResponse] containing context-specific recommendations and data to support response planning.
     *      1. Identify nearby active users and their distance away from incident.
     *      2. Suggest potential impacts based on the nature and context of the event.
     *      3. Suggest potential response tasks based on prior event-task patterns.
     *      4. Recommend personnel assignments for each task based on specialisation and proximity.
     *      5. Retrieve similar historical incidents for reference or comparison.
     */
    suspend fun findThreatResponse(
        incidentEventInput: EventDetailData,
        taskEventInput: EventDetailData
    ): ThreatAlertResponse

    /**
     * Retrieves all actions of a user's session.
     * @param userIdentifier Identifier of the user.
     * @return A map of the timestamp to user's action.
     */
    fun queryUserActions(userIdentifier: String): Map<Long, String>


}