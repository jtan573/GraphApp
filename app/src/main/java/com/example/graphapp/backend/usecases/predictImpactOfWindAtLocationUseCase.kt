package com.example.graphapp.backend.usecases

import android.util.Log
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.EventRepository
import com.example.graphapp.backend.dto.GraphSchema.SchemaPropertyNodes
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

suspend fun predictImpactOfWindAtLocationUseCase(
    stationCoordinates: List<String>,
    eventRepository: EventRepository,
    embeddingRepository: EmbeddingRepository
): List<EventDetails>? {

    val windNode = eventRepository.getEventNodesByType("Wind")?.first()
    if (windNode == null) return null

    val incidentNodes = eventRepository.getEventNodesByType("Incident")

    val incidentsList = mutableListOf<EventDetails>()
    incidentNodes?.forEach { incident ->
        val incidentLoc = eventRepository.getNeighborsOfEventNodeById(incident.id)
            .firstOrNull { it.type == "Location" }?.name
        if (incidentLoc == null) return@forEach

        val checkWind = shouldCheckWind(incident.name, embeddingRepository)
        if (!checkWind) return@forEach

        val (incidentLat, incidentLon) = incidentLoc.split(",")
        stationCoordinates.forEach { station ->
            val (targetLat, targetLon) = station.split((","))

            val affected = isLocationDownwind(
                incidentLat.toDouble(), incidentLon.toDouble(),
                targetLat.toDouble(), targetLon.toDouble(),
                windNode.name)

            if (affected) {
                val alreadyAdded = incidentsList.any { it.eventId == incident.id }
                if (!alreadyAdded) {
                    incidentsList.add(EventDetails(
                        eventId = incident.id,
                        eventName = incident.name,
                        eventProperties = eventRepository.getNeighborsOfEventNodeById(incident.id)
                            .filter { it.type in SchemaPropertyNodes }
                            .associate { it.type to it.name },
                        simScore = 1f
                    ))
                }
            }
        }
    }
    return incidentsList
}

suspend fun shouldCheckWind(
    incidentDesc: String,
    embeddingRepository: EmbeddingRepository,
    threshold: Float = 0.7f,
) : Boolean {
    val query = "Incident involving airborne chemical release"
    val queryVec = embeddingRepository.getTextEmbeddings(query)
    val inputVec = embeddingRepository.getTextEmbeddings(incidentDesc)
    val score = embeddingRepository.cosineDistance(queryVec, inputVec)
    if (score >= threshold) {
        return true
    }
    return false
}

fun bearingFromOriginToTarget(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val dLon = Math.toRadians(lon2 - lon1)

    val x = sin(dLon) * cos(lat2Rad)
    val y = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)

    val initialBearing = atan2(x, y)
    return (Math.toDegrees(initialBearing) + 360) % 360
}

fun isLocationDownwind(
    originLat: Double, originLon: Double,
    targetLat: Double, targetLon: Double,
    windDirection: String,
    marginDegrees: Double = 30.0
): Boolean {
    val windToBearing = mapOf(
        "N" to 0.0, "NE" to 45.0, "E" to 90.0, "SE" to 135.0,
        "S" to 180.0, "SW" to 225.0, "W" to 270.0, "NW" to 315.0
    )


    val windBearing = windToBearing[windDirection.uppercase()] ?: return false
    val bearingToTarget = bearingFromOriginToTarget(originLat, originLon, targetLat, targetLon)

    // Compute shortest angle difference (modulo 360)
    val angleDiff = abs((bearingToTarget - windBearing + 180) % 360 - 180)
    Log.d("COMPUTATION", "angleDiff: $angleDiff to marginDegrees: $marginDegrees")
    return angleDiff <= marginDegrees
}
