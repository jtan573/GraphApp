package com.example.graphapp.data.api

import com.example.graphapp.backend.dto.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.dto.GraphSchema.SchemaKeyEventTypeNames
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class RequestData {

    @Serializable
    @SerialName("EventRequestData")
    data class EventRequestData(
        val eventType: SchemaKeyEventTypeNames? = null,
        val details: EventDetailData? = null
    ) : RequestData()

    @Serializable
    @SerialName("UserRequestData")
    data class UserRequestData(
        val userData: UserDetailData? = null,
    ) : RequestData()

    @Serializable
    @SerialName("ActionRequestData")
    data class ActionRequestData(
        val actionData: ActionDetailData? = null
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
    val howValue: String? = null,
    val eventMap: Map<SchemaEventTypeNames, String>? = null
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
    val description: String? = null,
    val location: String?= null
)