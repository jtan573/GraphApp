package com.example.graphapp.domain.usecases

import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.local.recommendEventsForProps
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import java.time.Instant
import java.text.SimpleDateFormat
import java.util.*

suspend fun findSimilarEventByLoc(
    statusEventMap: Map<String, String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository,
    queryKey: String,
): List<EventDetails>? {

    // Only want to watch for behaviour/location
    if (statusEventMap["Location"] == null || statusEventMap["Location"] == "") {
        return null
    }

    val locationMap = statusEventMap.filter{ it.key == "Location" }
    val (_, _, locationRecs) = recommendEventsForProps(
        locationMap, eventRepository, embeddingRepository, queryKey, getTopThreeResultsOnly = false
    )

    val locationResults = mutableListOf<EventDetails>()
    if (locationRecs.predictedEvents.isEmpty()) {
        return null
    } else {
        val similarEventsByLocation = locationRecs.predictedEvents["Incident"]
        if (similarEventsByLocation != null) {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mmX", Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC+8")

            locationResults.addAll(similarEventsByLocation.sortedBy { formatter.parse(it.eventProperties["Date"]!!) })
        }
    }

    return locationResults
}