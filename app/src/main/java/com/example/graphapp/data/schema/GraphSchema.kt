package com.example.graphapp.data.schema

object GraphSchema {
    val edgeLabels = mapOf(
        "Article-Entity" to "Who",
        "Article-Method" to "How",
        "Article-Date" to "When",
        "Article-Location" to "Where",
        "Article-Motive" to "Why"
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
