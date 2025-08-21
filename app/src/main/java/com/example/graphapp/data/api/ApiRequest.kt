package com.example.graphapp.data.api

import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
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