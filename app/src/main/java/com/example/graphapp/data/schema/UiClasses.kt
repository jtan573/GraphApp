package com.example.graphapp.data.schema

import com.example.graphapp.data.api.Recommendation
import com.example.graphapp.data.db.UserNodeEntity

data class Event(val fields: Map<String, String>)

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object NavigateBack : UiEvent()
}

enum class ActiveButton { NONE, EVENT, FILL, FIND }

sealed class QueryResult {
    data class IncidentResponse(
        val nearbyActiveUsersMap: Map<UserNodeEntity, Int>? = null,
        val potentialImpacts: List<Recommendation> = null,
        val possibleResponses: List<String>? = null
    ) : QueryResult()
}