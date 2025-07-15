package com.example.graphapp.ui.viewmodels

import com.example.graphapp.data.repository.VectorRepository
import com.example.graphapp.data.local.NodeEntity
import com.example.graphapp.data.local.computeSemanticMatrixForQuery
import com.example.graphapp.data.schema.GraphSchema.SchemaEdgeLabels
import com.example.graphapp.data.schema.GraphSchema.SchemaKeyNodes
import com.example.graphapp.data.schema.GraphSchema.SchemaPropertyNodes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

suspend fun addNewEventIntoDb(
    normalizedMap: Map<String, String>,
    vectorRepository: VectorRepository,
): MutableList<NodeEntity> {

    val newEventNodes = mutableListOf<NodeEntity>()

    for ((type, value) in normalizedMap) {
        vectorRepository.insertNodeIntoDb(inputName = value, inputType = type)
        newEventNodes.add(vectorRepository.getNodeByNameAndType(value, type)!!)
    }
    for ((type1, value1) in normalizedMap) {
        for ((type2, value2) in normalizedMap) {
            if (type1 != type2) {
                val edgeType = SchemaEdgeLabels["$type1-$type2"]
                if (edgeType != null) {
                    vectorRepository.insertEdgeIntoDB(
                        fromNode = vectorRepository.getNodeByNameAndType(value1, type1),
                        toNode = vectorRepository.getNodeByNameAndType(value2, type2)
                    )
                }
            }
        }
    }

    return newEventNodes
}

suspend fun prepareNewEventNodesAndMatrix(
    isDuplicateEvent: Boolean,
    duplicateNode: NodeEntity?,
    isQuery: Boolean,
    normalizedMap: Map<String, String>,
    vectorRepository: VectorRepository,
    simMatrix: Map<Pair<Long, Long>, Float>,
    reloadGraphData: suspend () -> Unit,
    reloadSimMatrix: suspend (VectorRepository, MutableMap<Pair<Long, Long>, Float>, Map<String, String>) -> Unit
): Pair<List<NodeEntity>, Map<Pair<Long, Long>, Float>> {

    val newEventNodes = mutableListOf<NodeEntity>()
    var filteredSimMatrix = emptyMap<Pair<Long, Long>, Float>()

    if (isDuplicateEvent && duplicateNode != null) {
        newEventNodes.add(duplicateNode)
        newEventNodes.addAll(
            vectorRepository.getNeighborsOfNodeById(duplicateNode.id)
                .filter { it.type in SchemaPropertyNodes }
        )
    } else if (!isQuery) {
        // Insert nodes
        val eventNodesCreated = addNewEventIntoDb(normalizedMap, vectorRepository)
        newEventNodes.addAll(eventNodesCreated)

        // Reload graph
        withContext(Dispatchers.Default) {
            reloadGraphData()
        }

        // Reload similarity matrix
        reloadSimMatrix(
            vectorRepository, simMatrix.toMutableMap(), normalizedMap
        )
    } else {
        // Query
        val keyNodeType = normalizedMap.filter { it.key in SchemaKeyNodes }
            .map { it.key }
            .single()

        val (filteredSemSimMatrix, eventNodesCreated) = computeSemanticMatrixForQuery(
            vectorRepository, simMatrix, normalizedMap, keyNodeType
        )
        filteredSimMatrix = filteredSemSimMatrix
        newEventNodes.addAll(eventNodesCreated)
    }

    return newEventNodes to filteredSimMatrix
}
