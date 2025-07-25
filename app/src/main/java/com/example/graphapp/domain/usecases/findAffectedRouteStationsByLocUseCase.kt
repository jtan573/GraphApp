package com.example.graphapp.domain.usecases

import android.util.Log
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.local.predictMissingProperties
import com.example.graphapp.data.local.recommendEventsForProps
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.schema.GraphSchema.SchemaPropertyNodes
import com.example.graphapp.data.schema.QueryResult.IncidentResponse

suspend fun findAffectedRouteStationsByLocUseCase(
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    routeStations: List<String>? = null,
    threshold: Float = 0.5f
) : Map<Pair<Int, String>, List<EventDetails>>? {

    if (routeStations == null || routeStations.isEmpty()) {
        return null
    }

    val allIncidentsFound = mutableMapOf<Pair<Int, String>, List<EventDetails>>()

    routeStations.forEachIndexed { index, station ->
        val incidentsFoundList = mutableListOf<EventDetails>()

        val (_, _, locationRecs) = recommendEventsForProps(
            newEventMap = mapOf("Location" to station),
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            queryKey = "Incident",
            getTopThreeResultsOnly = true,
            customThreshold = threshold
        )

        if (locationRecs.predictedEvents.isNotEmpty()) {
            val nearbyIncidents = locationRecs.predictedEvents["Incident"]
            if (nearbyIncidents != null) {
                incidentsFoundList.addAll(nearbyIncidents)
            }
        }


        val incidentNodes = eventRepository.getEventNodesByType("Incident")
        val windIncidents = mutableListOf<EventDetails>()

        incidentNodes?.forEach { incident ->

            val incidentLoc = eventRepository.getNeighborsOfEventNodeById(incident.id)
                .firstOrNull { it.type == "Location" }?.name

            if (incidentLoc == null) return@forEach

            val affectedByWind = predictImpactOfWindAtLocationUseCase(
                stationCoordinates = station,
                incidentMap = mapOf<String, String> (
                    "Incident" to incident.name, "Location" to incidentLoc
                ),
                eventRepository,
                embeddingRepository
            )

            if (affectedByWind.first != null && affectedByWind.first == true) {
                windIncidents.add(
                    EventDetails(
                        eventId = incident.id,
                        eventName = incident.name,
                        eventProperties = eventRepository.getNeighborsOfEventNodeById(incident.id)
                            .filter { it.type in SchemaPropertyNodes }
                            .associate { it.type to it.name },
                        simScore = 0f
                    )
                )
            }
        }
        if (windIncidents.isNotEmpty()) {
            incidentsFoundList.addAll(windIncidents)
        }

        allIncidentsFound.put(
            (index to station),
            incidentsFoundList
        )
    }

    return allIncidentsFound
}