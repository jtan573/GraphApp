package com.example.graphapp.frontend.viewmodels

//suspend fun addNewEventIntoDb(
//    normalizedMap: Map<String, String>,
//    vectorRepository: EventRepository,
//): MutableList<EventNodeEntity> {
//
//    val newEventNodes = mutableListOf<EventNodeEntity>()
//
//    for ((type, value) in normalizedMap) {
//        vectorRepository.insertEventNodeIntoDb(inputName = value, inputType = type)
//        newEventNodes.add(vectorRepository.getEventNodeByNameAndType(value, type)!!)
//    }
//    for ((type1, value1) in normalizedMap) {
//        for ((type2, value2) in normalizedMap) {
//            if (type1 != type2) {
//                val edgeType = SchemaEdgeLabels["$type1-$type2"]
//                if (edgeType != null) {
//                    vectorRepository.insertEventEdgeIntoDb(
//                        fromNode = vectorRepository.getEventNodeByNameAndType(value1, type1),
//                        toNode = vectorRepository.getEventNodeByNameAndType(value2, type2)
//                    )
//                }
//            }
//        }
//    }
//
//    return newEventNodes
//}

//suspend fun prepareNewEventNodesAndMatrix(
//    isDuplicateEvent: Boolean,
//    duplicateNode: EventNodeEntity?,
//    isQuery: Boolean,
//    normalizedMap: Map<String, String>,
//    eventRepository: EventRepository,
//    embeddingRepository: EmbeddingRepository,
//    simMatrix: Map<Pair<Long, Long>, Float>,
//    reloadGraphData: suspend () -> Unit,
//    reloadSimMatrix: suspend (MutableMap<Pair<Long, Long>, Float>, Map<String, String>) -> Unit
//): Pair<List<EventNodeEntity>, Map<Pair<Long, Long>, Float>> {
//
//    val newEventNodes = mutableListOf<EventNodeEntity>()
//    var filteredSimMatrix = emptyMap<Pair<Long, Long>, Float>()
//
//    if (isDuplicateEvent && duplicateNode != null) {
//        newEventNodes.add(duplicateNode)
//        newEventNodes.addAll(
//            eventRepository.getNeighborsOfEventNodeById(duplicateNode.id)
//                .filter { it.type in SchemaPropertyNodes }
//        )
//    } else if (!isQuery) {
//        // Insert nodes
//        val eventNodesCreated = addNewEventIntoDb(normalizedMap, eventRepository)
//        newEventNodes.addAll(eventNodesCreated)
//
//        // Reload graph
//        withContext(Dispatchers.Default) {
//            reloadGraphData()
//        }
//
//        // Reload similarity matrix
//        reloadSimMatrix(
//            simMatrix.toMutableMap(), normalizedMap
//        )
//    } else {
//        // Query
//        val keyNodeType = normalizedMap.filter { it.key in SchemaKeyNodes }
//            .map { it.key }
//            .single()
//
//        val (filteredSemSimMatrix, eventNodesCreated) = computeSemanticMatrixForQuery(
//            eventRepository, embeddingRepository, simMatrix, normalizedMap, keyNodeType
//        )
//        filteredSimMatrix = filteredSemSimMatrix
//        newEventNodes.addAll(eventNodesCreated)
//    }
//
//    return newEventNodes to filteredSimMatrix
//}
