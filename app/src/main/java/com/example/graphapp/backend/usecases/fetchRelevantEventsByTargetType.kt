package com.example.graphapp.backend.usecases

import com.example.graphapp.backend.core.computeSimilarAndRelatedEvents
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository

/* ------------------------------------------------------------------
    Function to query for relevant incidents (Default Incidents)
------------------------------------------------------------------ */
suspend fun fetchRelevantEventsByTargetType(
    statusEventMap: Map<SchemaEventTypeNames, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    sourceEventType: SchemaKeyEventTypeNames? = null,
    targetEventType: SchemaKeyEventTypeNames? = null,
    activeNodesOnly: Boolean
): Map<SchemaKeyEventTypeNames, List<EventDetails>> {

    val resultsRecs = computeSimilarAndRelatedEvents(
        newEventMap = statusEventMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        sourceEventType = sourceEventType,
        targetEventType = targetEventType,
        activeNodesOnly = activeNodesOnly
    )

    return resultsRecs
}
