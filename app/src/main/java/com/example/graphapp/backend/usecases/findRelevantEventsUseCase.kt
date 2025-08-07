package com.example.graphapp.backend.usecases

import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.backend.core.recommendEventsForProps
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository

/* ------------------------------------------------------------------
    Function to query for relevant incidents (Default Incidents)
------------------------------------------------------------------ */
suspend fun findRelevantEventsUseCase(
    statusEventMap: Map<String, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    queryKey: String = "Incident",
    activeNodesOnly: Boolean
): Triple<List<EventNodeEntity>, List<EventEdgeEntity>, Any> {

    var nodes = listOf<EventNodeEntity>()
    var edges = listOf<EventEdgeEntity>()

    val (resultsNodes, resultsEdges, resultsRecs) = recommendEventsForProps(
        newEventMap = statusEventMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        targetEventType = queryKey,
        activeNodesOnly = activeNodesOnly
    )
    if (resultsNodes != null && resultsEdges != null) {
        nodes = resultsNodes
        edges = resultsEdges
    }

    return Triple(nodes, edges, resultsRecs)
}
