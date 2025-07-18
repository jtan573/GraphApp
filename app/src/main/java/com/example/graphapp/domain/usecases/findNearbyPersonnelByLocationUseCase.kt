package com.example.graphapp.domain.usecases

import android.location.Location
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.UserActionRepository

suspend fun findNearbyPersonnelByLocationUseCase(
   userActionRepository: UserActionRepository,
   embeddingRepository: EmbeddingRepository,
   threatLocation: String,
   threatDescription: String? = null,
   radiusInMeters: Float,
): Map<UserNodeEntity, Int> {

    val restoredThreatLocation = restoreLocationFromString(threatLocation)
    val allActiveUsers = userActionRepository.getAllUserNodes()

    lateinit var threatEmbedding: FloatArray
    if (threatDescription != null) {
        threatEmbedding = embeddingRepository.getTextEmbeddings(threatDescription)
    }

    val nearbyPersonnel = mutableListOf<Triple<UserNodeEntity, Int, Float>>()
    for (user in allActiveUsers) {
        val locationOfUser = restoreLocationFromString(user.currentLocation)
        val distance = restoredThreatLocation.distanceTo(locationOfUser)

        if (distance <= radiusInMeters && threatEmbedding.isNotEmpty()) {
            val cosSim = embeddingRepository.cosineDistance(threatEmbedding, user.embedding!!)
            nearbyPersonnel.add(Triple(user, distance.toInt(), cosSim))
        }
    }

    return nearbyPersonnel.sortedByDescending { it.third }.associate {
        it.first to it.second
    }
}

private fun restoreLocationFromString(coordinates: String): Location {
    val parts = coordinates.split(",")
    val lat = parts[0].toDouble()
    val lon = parts[1].toDouble()
    return Location("").apply {
        latitude = lat
        longitude = lon
    }
}