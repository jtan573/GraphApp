package com.example.graphapp.backend.usecases

import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.backend.core.computeSimilarAndRelatedEvents
import com.example.graphapp.backend.dto.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.dto.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.data.api.DiscoverEventsResponse
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
    queryKey: SchemaKeyEventTypeNames? = null,
    activeNodesOnly: Boolean
): Triple<List<EventNodeEntity>, List<EventEdgeEntity>, DiscoverEventsResponse> {

    var nodes = listOf<EventNodeEntity>()
    var edges = listOf<EventEdgeEntity>()

    val (resultsNodes, resultsEdges, resultsRecs) = computeSimilarAndRelatedEvents(
        newEventMap = statusEventMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        sourceEventType = sourceEventType,
        targetEventType = queryKey,
        activeNodesOnly = activeNodesOnly
    )
    if (resultsNodes != null && resultsEdges != null) {
        nodes = resultsNodes
        edges = resultsEdges
    }

    return Triple(nodes, edges, resultsRecs)
}
