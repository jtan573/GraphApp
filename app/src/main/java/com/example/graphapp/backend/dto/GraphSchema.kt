package com.example.graphapp.backend.dto

object GraphSchema {
    val SchemaEdgeLabels = mapOf(
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

        "Task-Incident" to "Resolves",
        "Outcome-Incident" to "Outcome_Of",
        "Impact-Incident" to "Impact_Of",
//        "Description-Entity" to "Description",
//        "Description-Method" to "Description",

        "Location-Wind" to "Where",
        "Date-Wind" to "When",
    )

    val SchemaKeyNodes = listOf(
        "Task", "Incident", "Outcome", "Impact"
    )

    val SchemaPropertyNodes = listOf(
        "Entity", "Method", "Location", "Motive"
    )

    val SchemaSemanticPropertyNodes = listOf(
        "Entity", "Method", "Motive", "Task", "Incident", "Outcome", "Impact"
    )

    val SchemaComputedPropertyNodes = listOf(
        "Location",
    )

    val SchemaOtherNodes = listOf(
        "Date", "Description"
    )

    val SchemaConditionsNodes = listOf(
        "Wind"
    )
}