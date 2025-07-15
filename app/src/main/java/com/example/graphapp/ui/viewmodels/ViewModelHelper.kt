package com.example.graphapp.ui.viewmodels

import com.example.graphapp.data.VectorRepository
import com.example.graphapp.data.local.NodeEntity
import com.example.graphapp.data.schema.GraphSchema.SchemaEdgeLabels
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