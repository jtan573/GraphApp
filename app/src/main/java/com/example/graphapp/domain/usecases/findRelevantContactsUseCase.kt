package com.example.graphapp.domain.usecases

import android.util.Log
import com.example.graphapp.data.repository.EmbeddingRepository
import com.example.graphapp.data.repository.UserActionRepository

suspend fun findRelevantContactsUseCase(
    eventDescription: String,
    userActionRepository: UserActionRepository,
    embeddingRepository: EmbeddingRepository
): List<Triple<String, String, Float>> {

    val eventDescEmbed = embeddingRepository.getTextEmbeddings(eventDescription)
    val allActiveUsers = userActionRepository.getAllUserNodes()

    val topRelevantUsers = mutableListOf<Triple<String, String, Float>>()
    for (user in allActiveUsers) {
        val simScore = embeddingRepository.cosineDistance(eventDescEmbed, user.embedding!!)
        if (simScore > 0.5) {
            topRelevantUsers.add(Triple(user.identifier, user.specialisation, simScore))
        }
    }

    val sortedUsers = topRelevantUsers.sortedByDescending { it.third }.take(3)
    Log.d("TOP-RELEVANT-USERS", "top relevant users: $sortedUsers")

    return sortedUsers
}