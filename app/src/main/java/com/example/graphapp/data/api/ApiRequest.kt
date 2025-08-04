package com.example.graphapp.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class DbAction {
    CREATE, UPDATE, DELETE
}

data class ApiRequest(
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: DbAction,
    val inputData: RequestData
)

@Serializable
sealed class RequestData {

    @Serializable
    @SerialName("EventRequestData")
    data class EventRequestData(
        val eventType: String? = null,
        val details: EventDetailData? = null,
        val metadata: RequestEntry? = null
    ) : RequestData()

    @Serializable
    @SerialName("PersonnelRequestData")
    data class PersonnelRequestData(
        val whereValue: String? = null,
        val descValue: String? = null
    ) : RequestData()
}

/* -------------------------------------------------
    Related to Events
------------------------------------------------- */
@Serializable
data class EventDetailData(
    val whatValue: String? = null,
    val whoValue: String? = null,
    val whenValue: String? = null,
    val whereValue: String? = null,
    val whyValue: String? = null,
    val howValue: String? = null
)

@Serializable
data class RequestEntry(
    val routeCoordinates: List<String>? = null
)

/* -------------------------------------------------
    Related to Users/Personnel
------------------------------------------------- */
