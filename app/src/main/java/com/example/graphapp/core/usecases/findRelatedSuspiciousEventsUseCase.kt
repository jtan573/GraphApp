package com.example.graphapp.core.usecases

import android.util.Log
import com.example.graphapp.core.analyser.EventEmbeddingSet
import com.example.graphapp.core.analyser.computeSimilarAndRelatedEvents
import com.example.graphapp.core.schema.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.core.schema.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.core.model.dto.EventDetailData
import com.example.graphapp.core.model.dto.EventDetails
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import javax.inject.Inject

/**
 * Implemented for Use Case 4: Suspicious Pattern Detection
 * Checks for recent incidents with similar behaviour and location.
 */
suspend fun findRelatedSuspiciousEventsUseCase (
    eventInput: EventDetailData,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository
) : Map<SchemaKeyEventTypeNames, List<EventDetails>> {

    val similarIncidentsFound = mutableListOf<EventDetails>()
    val simEventsLocAndDate = mutableListOf<Pair<String, String>>()

    val incidentName = eventInput.whatValue
    val incidentMethod = eventInput.howValue
    val incidentLocation = eventInput.whereValue
    val incidentDateTime = eventInput.whenValue

    if ((incidentMethod != null || incidentName != null) && incidentLocation != null) {
        val results = computeSimilarAndRelatedEvents(
            newEventMap = buildMap<SchemaEventTypeNames, String> {
                incidentName?.let { put(SchemaEventTypeNames.INCIDENT, it) }
                incidentMethod?.let { put(SchemaEventTypeNames.HOW, it) }
                incidentDateTime?.let { put(SchemaEventTypeNames.WHEN, it) }
                put(SchemaEventTypeNames.WHERE, incidentLocation)
            },
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            sourceEventType = SchemaKeyEventTypeNames.INCIDENT,
            targetEventType = SchemaKeyEventTypeNames.INCIDENT,
            activeNodesOnly = false,
            numTopResults = 5
        )
        val similarIncidents = results[SchemaKeyEventTypeNames.INCIDENT]
        if (!similarIncidents.isNullOrEmpty()) {
            similarIncidentsFound.addAll(similarIncidents)
            similarIncidents.forEach {
                val locFound = eventRepository.getNeighborsOfEventNodeById(it.eventId)
                    .single { it.type == SchemaEventTypeNames.WHERE.key }.name
                val dateFound = eventRepository.getNeighborsOfEventNodeById(it.eventId)
                    .single { it.type == SchemaEventTypeNames.WHEN.key }.name
                if (locFound != "" && dateFound != "") {
                    simEventsLocAndDate.add(locFound to dateFound)
                }
            }
        }
    }
    similarIncidentsFound.forEach {
        Log.d("check", "SUSPICIOUS INCIDENT: ${it.eventName}")
    }

    return mapOf(SchemaKeyEventTypeNames.INCIDENT to similarIncidentsFound)
}