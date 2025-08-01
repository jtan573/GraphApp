package com.example.graphapp.data.api

enum class DbAction {
    CREATE, UPDATE, DELETE
}

data class ApiRequest(
    val userId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: DbAction,
    val inputData: RequestData
)

sealed class RequestData {
    data class EventRequestData(
        val eventType: String,
        val details: EventDetailData,
        val metadata: RequestEntry? = null
    ) : RequestData()

    data class PersonnelRequestData(
        val whereValue: String? = null,
        val descValue: String? = null
    ) : RequestData()
}

/* -------------------------------------------------
    Related to Events
------------------------------------------------- */
data class EventDetailData(
    val whatValue: String? = null,
    val whoValue: String? = null,
    val whenValue: String? = null,
    val whereValue: String? = null,
    val whyValue: String? = null,
    val howValue: String? = null
)

data class RequestEntry(
    val routeCoordinates: List<String>? = null
)

/* -------------------------------------------------
    Related to Users/Personnel
------------------------------------------------- */
