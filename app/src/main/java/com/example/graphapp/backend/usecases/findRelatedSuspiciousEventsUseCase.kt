package com.example.graphapp.backend.usecases

import android.util.Log
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.backend.core.computeSimilarAndRelatedEvents
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
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
    val simEventsLocAndDate = mutableListOf<Pair<String, String>>()

    val incidentName = eventInput.whatValue
    val incidentMethod = eventInput.howValue
    val incidentLocation = eventInput.whereValue
    val incidentDateTime = eventInput.whenValue

    if ((incidentMethod != null || incidentName != null) && incidentLocation != null) {
        val (_, _, similarIncidents) = computeSimilarAndRelatedEvents(
            newEventMap = buildMap<SchemaEventTypeNames, String> {
                incidentName?.let { put(SchemaEventTypeNames.INCIDENT, it) }
                incidentMethod?.let { put(SchemaEventTypeNames.HOW, it) }
                put(SchemaEventTypeNames.WHERE, incidentLocation)
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
                similarEventsByMethod.forEach {
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
    }

    // test
    if (incidentDateTime != null && incidentLocation != null) {
        simEventsLocAndDate.add(incidentLocation to incidentDateTime)
    }

    simEventsLocAndDate.forEach { (loc, date) ->
        val (_, _, similarIncidentsByDatetime) = computeSimilarAndRelatedEvents(
            newEventMap = buildMap<SchemaEventTypeNames, String> {
                put(SchemaEventTypeNames.INCIDENT, "suspicious")
                put(SchemaEventTypeNames.WHEN, date)
                put(SchemaEventTypeNames.WHERE, loc)
            },
            eventRepository = eventRepository,
            embeddingRepository = embeddingRepository,
            sourceEventType = SchemaKeyEventTypeNames.INCIDENT,
            targetEventType = SchemaKeyEventTypeNames.INCIDENT,
            activeNodesOnly = false
        )
        similarIncidentsByDatetime.predictedEvents.forEach { (type, events) ->
            events.forEach { newEvent ->
                if (similarIncidentsFound.none { it.eventId == newEvent.eventId }) {
                    similarIncidentsFound.add(newEvent)
                }
            }
        }
    }
    similarIncidentsFound.forEach {
        Log.d("check", "SUSPICIOUS INCIDENT: ${it.eventName}")
    }
    return similarIncidentsFound
}