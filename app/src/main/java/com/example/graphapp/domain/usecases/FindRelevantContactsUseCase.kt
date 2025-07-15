package com.example.graphapp.domain.usecases

import android.util.Log
import com.example.graphapp.data.api.ApiResponse
import com.example.graphapp.data.api.EventRecommendationResult
import com.example.graphapp.data.api.ResponseData
import com.example.graphapp.data.local.EdgeEntity
import com.example.graphapp.data.local.NodeEntity
import com.example.graphapp.data.local.computeSemanticMatrixForQuery
import com.example.graphapp.data.local.recommendEventForEvent
import com.example.graphapp.data.local.recommendEventsForProps
import com.example.graphapp.data.repository.VectorRepository
import com.example.graphapp.data.schema.GraphSchema.SchemaKeyNodes
import com.example.graphapp.domain.model.ContactRecommendation
import org.w3c.dom.Node

/*
Function to query for relevant contacts based on status.
 */
suspend fun findRelevantContactsUseCase(
    statusEventMap: Map<String, String>,
    repository: VectorRepository,
    simMatrix: Map<Pair<Long, Long>, Float>,
    queryKey: String = "Contacts"
): Triple<List<NodeEntity>, List<EdgeEntity>, EventRecommendationResult> {

    val noKeyTypes = statusEventMap.keys.none { it in SchemaKeyNodes }

    var nodes = listOf<NodeEntity>()
    var edges = listOf<EdgeEntity>()
    lateinit var result: EventRecommendationResult

    // For entries with no key nodes
    if (noKeyTypes) {
        val (resultsNodes, resultsEdges, resultsRecs) = recommendEventsForProps(statusEventMap, repository)
        nodes = resultsNodes
        edges = resultsEdges
        result = resultsRecs

    } else {
        val newEventNodes = mutableListOf<NodeEntity>()
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
        result = resultsRecs
    }

    return Triple(nodes, edges, result)
}
