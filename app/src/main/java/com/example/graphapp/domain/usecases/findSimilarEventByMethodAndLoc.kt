package com.example.graphapp.domain.usecases

import android.util.Log
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.local.recommendEventsForProps
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository

suspend fun findSimilarEventByMethodAndLoc(
    statusEventMap: Map<String, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    queryKey: String
): MutableMap<String, List<EventDetails>>? {

    // Only want to watch for behaviour/location
    if (statusEventMap["Method"] == "" && statusEventMap["Location"] == "") {
        return null
    }

    // Find events with similar behaviour
    val methodMap = statusEventMap.filter{ it.key == "Method" }
    val (_, _, methodRecs) = recommendEventsForProps(
        methodMap, eventRepository, embeddingRepository, queryKey
    )

    val locationMap = statusEventMap.filter{ it.key == "Location" }
    val (_, _, locationRecs) = recommendEventsForProps(
        locationMap, eventRepository, embeddingRepository, queryKey
    )

    val behaviourResults = mutableMapOf<String, List<EventDetails>>()

    if (methodRecs.predictedEvents.isEmpty() && locationRecs.predictedEvents.isEmpty()) {
        return null
    }

    if (methodRecs.predictedEvents.isNotEmpty()) {
        val similarEventsByMethod = methodRecs.predictedEvents["Incident"]
        if (similarEventsByMethod != null) {
            behaviourResults.put("Method", similarEventsByMethod)
        }
    }

    if (locationRecs.predictedEvents.isNotEmpty()) {
        val similarEventsByLocation = locationRecs.predictedEvents["Incident"]
        if (similarEventsByLocation != null) {
            behaviourResults.put("Location", similarEventsByLocation)
        }
    }

    return behaviourResults
}