package com.example.graphapp.data.api

import com.example.graphapp.backend.dto.GraphSchema.PropertyNames
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class DbAction {
    CREATE, DELETE, QUERY
}

enum class EventType(val key: String) {
    INCIDENT("Incident"),
    TASK("Task"),
    OUTCOME("Outcome"),
    IMPACT("Impact");

    companion object {
        fun fromKey(key: String): EventType? =
            entries.find { it.key.equals(key, ignoreCase = true) }

        fun toKey(type: EventType): String = type.key
    }
}

fun eventTypeFromString(key: String): EventType? {
    return EventType.entries.find { it.name.equals(key, ignoreCase = true) }
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
        val eventType: EventType? = null,
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
    val eventMap: Map<PropertyNames, String>? = null
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