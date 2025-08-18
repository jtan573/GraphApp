package com.example.graphapp.backend.usecases

import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.backend.core.computeSimilarAndRelatedEvents
import com.example.graphapp.backend.dto.GraphSchema.PropertyNames
import com.example.graphapp.data.api.EventDetailData
import com.example.graphapp.data.api.EventType
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository

/* --------------------------------------------------
    Function to suspicious events in database
-------------------------------------------------- */
suspend fun findRelatedSuspiciousEventsUseCase(
    eventInput: EventDetailData,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository
) : List<EventDetails> {

    val similarIncidentsFound = mutableListOf<EventDetails>()

    val incidentName = eventInput.whatValue
    val incidentMethod = eventInput.howValue
    val incidentLocation = eventInput.whereValue
    if ((incidentMethod != null || incidentName != null) && incidentLocation != null) {
        val (_, _, similarIncidents) = computeSimilarAndRelatedEvents(
            newEventMap = buildMap<PropertyNames, String> {
                incidentName?.let { put(PropertyNames.INCIDENT, it) }
                incidentMethod?.let { put(PropertyNames.HOW, it) }
                PropertyNames.WHERE to incidentLocation
            },
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            sourceEventType = EventType.INCIDENT,
            targetEventType = EventType.INCIDENT,
            activeNodesOnly = false
        )

        if (similarIncidents.predictedEvents.isNotEmpty()) {
            val similarEventsByMethod = similarIncidents.predictedEvents[EventType.INCIDENT]
            if (similarEventsByMethod != null) {
                similarIncidentsFound.addAll(similarEventsByMethod)
            }
        }
    }

    return similarIncidentsFound
}