package com.example.graphapp.backend.usecases

import android.location.Location
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.UserActionRepository

suspend fun findRelevantPersonnelByLocationUseCase(
   userActionRepository: UserActionRepository,
   embeddingRepository: EmbeddingRepository,
   threatLocation: String? = null,
   threatDescription: String? = null,
   radiusInMeters: Float,
): List<Triple<UserNodeEntity, Int, Float>>? {

    if (threatLocation == null) {
        return null
    }

    val restoredThreatLocation = restoreLocationFromString(threatLocation)
    val allActiveUsers = userActionRepository.getAllUserNodes()

    var threatEmbedding: FloatArray? = null
    if (threatDescription != null) {
        threatEmbedding = embeddingRepository.getTextEmbeddings(threatDescription)
    }

    val nearbyPersonnel = mutableListOf<Triple<UserNodeEntity, Int, Float>>()
    for (user in allActiveUsers) {
        val locationOfUser = restoreLocationFromString(user.currentLocation)
        val distance = restoredThreatLocation.distanceTo(locationOfUser)

        if (distance <= radiusInMeters) {
            if (threatEmbedding != null) {
                val cosSim = embeddingRepository.computeCosineSimilarity(threatEmbedding, user.embedding!!)
                nearbyPersonnel.add(Triple(user, distance.toInt(), cosSim))
            } else {
                nearbyPersonnel.add(Triple(user, distance.toInt(), 0f))
            }
        }
    }

    return nearbyPersonnel.sortedByDescending { it.third }.take(3)
}

fun restoreLocationFromString(coordinates: String): Location {
    val parts = coordinates.split(",")
    val lat = parts[0].toDouble()
    val lon = parts[1].toDouble()
    return Location("").apply {
        latitude = lat
        longitude = lon
    }
}