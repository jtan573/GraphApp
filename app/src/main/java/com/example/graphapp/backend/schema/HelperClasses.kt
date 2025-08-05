package com.example.graphapp.backend.schema

import io.objectbox.converter.PropertyConverter

data class EventEmbeddingSet (
    // Type, Metadata
    val semanticProps: Map<String, EventMetadata>? = null,
    val computedProps: Map<String, EventMetadata>? = null
)

data class EventMetadata (
    val eventName: String,
    val eventEmbeddings: FloatArray? = null,
    val eventTags: List<String>? = null
)

// For explained similarity
data class SimilarEventTags (
    val propertyType: String,
    val tagsA: List<String>,
    val tagsB: List<String>,
    val relevantTagsA: List<String>,
    val relevantTagsB: List<String>
)

// For sim matrix
data class ExplainedSimilarityWithScores (
    val simScore: Float,
    val targetNodeId: Long,
    val explainedSimilarity: List<SimilarEventTags>
)


// enum class for vector db
enum class EventStatus(val id: Int) {
    INACTIVE(0), ACTIVE(1), UNKNOWN(2)
}

