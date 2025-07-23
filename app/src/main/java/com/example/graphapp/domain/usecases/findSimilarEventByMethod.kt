@file:JvmName("FindSimilarEventByMethodAndLocKt")

package com.example.graphapp.domain.usecases

import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.local.recommendEventsForProps
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository

suspend fun findSimilarEventByMethod(
    statusEventMap: Map<String, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    queryKey: String
): List<EventDetails>? {

    // Only want to watch for behaviour/location
    if (statusEventMap["Method"] == null || statusEventMap["Method"] == "") {
        return null
    }

    // Find events with similar behaviour
    val methodMap = statusEventMap.filter{ it.key == "Method" }
    val (_, _, methodRecs) = recommendEventsForProps(
        methodMap, eventRepository, embeddingRepository, queryKey
    )

    val methodResults = mutableListOf<EventDetails>()

    if (methodRecs.predictedEvents.isEmpty()) {
        return null
    } else {
        val similarEventsByMethod = methodRecs.predictedEvents["Incident"]
        if (similarEventsByMethod != null) {
            methodResults.addAll(similarEventsByMethod)
        }
    }

    return methodResults
}