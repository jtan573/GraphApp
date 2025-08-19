package com.example.graphapp.backend.usecases

import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.backend.core.computeSimilarAndRelatedEvents
import com.example.graphapp.backend.dto.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.dto.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.data.api.EventDetailData
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
            newEventMap = buildMap<SchemaEventTypeNames, String> {
                incidentName?.let { put(SchemaEventTypeNames.INCIDENT, it) }
                incidentMethod?.let { put(SchemaEventTypeNames.HOW, it) }
                SchemaEventTypeNames.WHERE to incidentLocation
            },
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            sourceEventType = SchemaKeyEventTypeNames.INCIDENT,
            targetEventType = SchemaKeyEventTypeNames.INCIDENT,
            activeNodesOnly = false
        )

        if (similarIncidents.predictedEvents.isNotEmpty()) {
            val similarEventsByMethod = similarIncidents.predictedEvents[SchemaKeyEventTypeNames.INCIDENT]
            if (similarEventsByMethod != null) {
                similarIncidentsFound.addAll(similarEventsByMethod)
            }
        }
    }

    return similarIncidentsFound
}