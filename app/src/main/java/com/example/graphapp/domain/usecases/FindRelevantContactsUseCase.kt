package com.example.graphapp.domain.usecases

import com.example.graphapp.data.db.EventEdgeEntity
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.local.computeSemanticMatrixForQuery
import com.example.graphapp.data.local.recommendEventForEvent
import com.example.graphapp.data.local.recommendEventsForProps
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.schema.GraphSchema.SchemaKeyNodes

/*
Function to query for relevant contacts based on status.
 */
suspend fun findRelevantIncidentsUseCase(
    statusEventMap: Map<String, String>,
    repository: EventRepository,
    simMatrix: Map<Pair<Long, Long>, Float>,
    queryKey: String = "Incident"
): Triple<List<EventNodeEntity>, List<EventEdgeEntity>, Any> {

    val noKeyTypes = statusEventMap.keys.none { it in SchemaKeyNodes }

    var nodes = listOf<EventNodeEntity>()
    var edges = listOf<EventEdgeEntity>()
    lateinit var result: Any

    // For entries with no key nodes
    if (noKeyTypes) {
        val (resultsNodes, resultsEdges, resultsRecs) = recommendEventsForProps(statusEventMap, repository, queryKey)
        if (resultsNodes != null && resultsEdges != null) {
            nodes = resultsNodes
            edges = resultsEdges
        }

        return Triple(nodes, edges, resultsRecs)

    } else {
        val newEventNodes = mutableListOf<EventNodeEntity>()
        var filteredSimMatrix = mapOf<Pair<Long, Long>, Float>()

        val keyNodeType = statusEventMap.filter { it.key in SchemaKeyNodes }
            .map { it.key }.single()

        val (filteredSemSimMatrix, eventNodesCreated) = computeSemanticMatrixForQuery(
            repository, simMatrix, statusEventMap, keyNodeType
        )
        filteredSimMatrix = filteredSemSimMatrix
        newEventNodes.addAll(eventNodesCreated)

        val (resultsNodes, resultsEdges, resultsRecs) = recommendEventForEvent(
            statusEventMap, repository, filteredSimMatrix, newEventNodes, queryKey, true
        )

        nodes = resultsNodes
        edges = resultsEdges

        return Triple(nodes, edges, resultsRecs)
    }

    return Triple(nodes, edges, result)
}
