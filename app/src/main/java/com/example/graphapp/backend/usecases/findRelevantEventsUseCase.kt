package com.example.graphapp.backend.usecases

import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.backend.core.recommendEventsForProps
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository

/*
Function to query for relevant contacts based on status.
 */
suspend fun findRelevantEventsUseCase(
    statusEventMap: Map<String, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    queryKey: String = "Incident",
    activeNodesOnly: Boolean
): Triple<List<EventNodeEntity>, List<EventEdgeEntity>, Any> {

//    val noKeyTypes = statusEventMap.keys.none { it in SchemaKeyNodes }

    var nodes = listOf<EventNodeEntity>()
    var edges = listOf<EventEdgeEntity>()
    lateinit var result: Any

    val (resultsNodes, resultsEdges, resultsRecs) = recommendEventsForProps(
        newEventMap = statusEventMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        queryKey = queryKey,
        activeNodesOnly = activeNodesOnly
    )
    if (resultsNodes != null && resultsEdges != null) {
        nodes = resultsNodes
        edges = resultsEdges
    }

    return Triple(nodes, edges, resultsRecs)

    // For entries with no key nodes
//    if (noKeyTypes) {
//        val (resultsNodes, resultsEdges, resultsRecs) = recommendEventsForProps(
//            statusEventMap, eventRepository, embeddingRepository, queryKey
//        )
//        if (resultsNodes != null && resultsEdges != null) {
//            nodes = resultsNodes
//            edges = resultsEdges
//        }
//
//        return Triple(nodes, edges, resultsRecs)
//
//    } else {
//        val newEventNodes = mutableListOf<EventNodeEntity>()
//        var filteredSimMatrix = mapOf<Pair<Long, Long>, Float>()
//
//        val keyNodeType = statusEventMap.filter { it.key in SchemaKeyNodes }
//            .map { it.key }.single()
//
//        val (filteredSemSimMatrix, eventNodesCreated) = computeSemanticMatrixForQuery(
//            eventRepository, embeddingRepository, simMatrix, statusEventMap, keyNodeType
//        )
//        filteredSimMatrix = filteredSemSimMatrix
//        newEventNodes.addAll(eventNodesCreated)
//
//        val (resultsNodes, resultsEdges, resultsRecs) = recommendEventForEvent(
//            statusEventMap, eventRepository, filteredSimMatrix, newEventNodes, queryKey, true
//        )
//
//        nodes = resultsNodes
//        edges = resultsEdges
//
//        return Triple(nodes, edges, resultsRecs)
//    }

    return Triple(nodes, edges, result)
}
