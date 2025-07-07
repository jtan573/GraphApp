package com.example.graphapp.data.schema

object GraphSchema {
    val edgeLabels = mapOf(
        "Entity-Article" to "Who",
        "Method-Article" to "How",
        "Date-Article" to "When",
        "Location-Article" to "Where",
        "Motive-Article" to "Why"
    )

    val keyNodes = listOf(
        "Article"
    )

    val propertyNodes = listOf(
        "Entity",
        "Method",
        "Location",
        "Motive"
    )

    val otherNodes = listOf(
        "Date",
        "Description"
    )
}
