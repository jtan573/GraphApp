package com.example.graphapp.core.analyser

import android.util.Log
import com.example.graphapp.core.schema.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.core.schema.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.core.schema.GraphSchema.SchemaOtherNodes
import com.example.graphapp.core.schema.GraphSchema.SchemaPropertyNodes
import com.example.graphapp.core.config.SimilarityConfig
import com.example.graphapp.core.model.dto.EventDetails
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

/**
 * Main function to compute similarity between events.
 *
 * @param eventRepository Repository of all events in the db.
 * @param embeddingRepository Repository of embedding model.
 * @param newEventMap Map of PropertyName to node representing the property.
 * @param sourceEventType Event type of input event.
 * @param targetEventType Event type of target event.
 * @param numTopResults Number of top results.
 * @param activeNodesOnly Boolean to control whether to consider all or only active nodes.
 * @return Map of key event type to list of similar events of that type found.
 */
suspend fun computeSimilarAndRelatedEvents (
    newEventMap: Map<SchemaEventTypeNames, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    sourceEventType: SchemaKeyEventTypeNames? = null,
    targetEventType: SchemaKeyEventTypeNames? = null,
    numTopResults: Int = SimilarityConfig.NUM_TOP_RESULTS_REQUIRED,
    activeNodesOnly: Boolean
) : Map<SchemaKeyEventTypeNames, List<EventDetails>> {

    var eventNodesByType = convertStringInputToNodes(
        newEventMap = newEventMap,
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository
    )

    val nodeStatusList = checkTargetNodeStatus(activeNodesOnly)
    val propsInEvent = eventNodesByType.values.toList()

    val allKeyNodeIdsByType = prepareDatasetForSimilarityComputation(
        newEventMap = eventNodesByType,
        eventRepository = eventRepository,
        nodeStatus = nodeStatusList,
        sourceEventType = sourceEventType
    )

    // For each key node type, compute top 3
    val topRecommendationsByType = generateSimilarityResults(
        eventRepository = eventRepository,
        embeddingRepository = embeddingRepository,
        allKeyNodeIdsByType = allKeyNodeIdsByType,
        targetEventType = targetEventType,
        numTopResults = numTopResults,
        propsInEvent = propsInEvent
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
