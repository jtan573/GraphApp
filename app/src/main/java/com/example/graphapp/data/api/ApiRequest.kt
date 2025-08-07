package com.example.graphapp.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class DbAction {
    CREATE, DELETE, QUERY
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
        val eventType: String? = null, // change to enum
        val details: EventDetailData? = null,
        val metadata: EventRequestEntry? = null
    ) : RequestData()

    @Serializable
    @SerialName("PersonnelRequestData")
    data class PersonnelRequestData(
        val userData: UserDetailData? = null,
        val actionData: ActionDetailData? = null,
        val metadata: PersonnelRequestEntry? = null
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
data class EventRequestEntry(
    val routeCoordinates: List<String>? = null
)

/* -------------------------------------------------
    Related to Users/Personnel
------------------------------------------------- */
@Serializable
data class UserDetailData (
    val identifier: String? = null,
    val role: String? = null,
    val specialisation: String? = null,
    val currentLocation: String? = null,
)

@Serializable
data class ActionDetailData (
    val actionName: String? = null,
    val userIdentifier: String? = null,
)

@Serializable
data class PersonnelRequestEntry (
    val incidentDescription: String? = null,
    val incidentLocation: String?= null
)