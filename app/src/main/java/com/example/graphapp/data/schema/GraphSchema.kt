package com.example.graphapp.data.schema

object GraphSchema {
    val edgeLabels = mapOf(
        "Entity-Task" to "Who",
        "Method-Task" to "How",
        "Date-Task" to "When",
        "Location-Task" to "Where",
        "Motive-Task" to "Why",

        "Entity-Incident" to "Who",
        "Method-Incident" to "How",
        "Date-Incident" to "When",
        "Location-Incident" to "Where",
        "Motive-Incident" to "Why",

        "Entity-Outcome" to "Who",
        "Method-Outcome" to "How",
        "Date-Outcome" to "When",
        "Location-Outcome" to "Where",
        "Motive-Outcome" to "Why",

        "Entity-Impact" to "Who",
        "Method-Impact" to "How",
        "Date-Impact" to "When",
        "Location-Impact" to "Where",
        "Motive-Impact" to "Why",

        "Incident-Task" to "Why",
        "Outcome-Incident" to "Why",
        "Impact-Incident" to "Why",
//        "Description-Entity" to "Description",
//        "Description-Method" to "Description",
    )

    val keyNodes = listOf(
        "Task", "Incident", "Outcome", "Impact"
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
