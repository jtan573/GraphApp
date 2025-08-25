package com.example.graphapp.backend.core

import android.util.Log
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.db.EventNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.backend.core.GraphSchema.SchemaOtherNodes
import com.example.graphapp.backend.core.GraphSchema.SchemaPropertyNodes
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

suspend fun computeSimilarAndRelatedEvents (
    newEventMap: Map<SchemaEventTypeNames, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    sourceEventType: SchemaKeyEventTypeNames? = null,
    targetEventType: SchemaKeyEventTypeNames? = null,
    numTopResults: Int = 3,
    customThreshold: Float = 0.0f,
    activeNodesOnly: Boolean
) : Map<SchemaKeyEventTypeNames, List<EventDetails>> {

    var eventNodesByType = mutableMapOf<String, EventNodeEntity>()

    // Get nodes from DB / Create temporary nodes for the search
    newEventMap.forEach { (type, value) ->
        val nodeExist = eventRepository.getEventNodeByNameAndType(value, type.key)
        if (nodeExist != null) {
            eventNodesByType[type.key] = nodeExist
        } else {
            eventNodesByType[type.key] = eventRepository.getTemporaryEventNode(value, type.key, embeddingRepository)
        }
    }

    // For each key node type, compute top 3
    val topRecommendationsByType = computeSemanticSimilarEventsForProps(
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        newEventMap = eventNodesByType,
        sourceEventType = sourceEventType,
        targetEventType = targetEventType,
        numTopResults = numTopResults,
        threshold = customThreshold,
        activeNodesOnly = activeNodesOnly
    )
    Log.d("topRecommendationsByType","$topRecommendationsByType")

    val eventsByType = mutableMapOf<SchemaKeyEventTypeNames, List<EventDetails>>()

    for ((type, recs) in topRecommendationsByType) {
        val predictedEventsList = mutableListOf<EventDetails>()
        for (rec in recs) {
            val neighbourProps = eventRepository.getNeighborsOfEventNodeById(rec.targetNodeId)
                .filter { it.type in (SchemaPropertyNodes + SchemaOtherNodes) }
                .associate { it.type to it.name }

            val recNode = eventRepository.getEventNodeById(rec.targetNodeId)!!
            predictedEventsList.add(
                EventDetails(
                    eventId = recNode.id,
                    eventName = recNode.name,
                    eventProperties = neighbourProps,
                    simScore = rec.simScore,
                    simProperties = rec.explainedSimilarity
                )
            )
        }
        eventsByType.put(SchemaKeyEventTypeNames.fromKey(type)!!, predictedEventsList.toList())
    }

    return eventsByType
}
