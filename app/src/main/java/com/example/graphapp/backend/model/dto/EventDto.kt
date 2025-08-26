package com.example.graphapp.backend.model.dto

import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.core.GraphSchema.SchemaKeyEventTypeNames
import com.example.graphapp.backend.core.SimilarEventTags
import kotlinx.serialization.Serializable

@Serializable
data class EventRequestData(
    val eventType: SchemaKeyEventTypeNames? = null,
    val details: EventDetailData? = null
)

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

data class EventDetails(
    val eventId: Long,
    val eventName: String,
    val eventProperties: Map<String, String>,
    val simScore: Float,
    val simProperties: List<SimilarEventTags>? = null
)
