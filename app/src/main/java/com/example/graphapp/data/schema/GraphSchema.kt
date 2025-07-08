package com.example.graphapp.data.schema

object GraphSchema {
    val edgeLabels = mapOf(
        "Entity-Article" to "Who",
        "Method-Article" to "How",
        "Date-Article" to "When",
        "Location-Article" to "Where",
        "Motive-Article" to "Why",
        "Description-Entity" to "Description",
        "Description-Method" to "Description",
        "Description-Date" to "Description",
        "Description-Location" to "Description",
        "Description-Motive" to "Description",
        "Description-Article" to "Description",
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
