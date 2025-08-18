package com.example.graphapp.backend.dto

object GraphSchema {
    val SchemaEdgeLabels = mapOf(
        "Entity-Task" to "Who",
        "Method-Task" to "How",
        "DateTime-Task" to "When",
        "Location-Task" to "Where",
        "Motive-Task" to "Why",

        "Entity-Incident" to "Who",
        "Method-Incident" to "How",
        "DateTime-Incident" to "When",
        "Location-Incident" to "Where",
        "Motive-Incident" to "Why",

        "Entity-Outcome" to "Who",
        "Method-Outcome" to "How",
        "DateTime-Outcome" to "When",
        "Location-Outcome" to "Where",
        "Motive-Outcome" to "Why",

        "Entity-Impact" to "Who",
        "Method-Impact" to "How",
        "DateTime-Impact" to "When",
        "Location-Impact" to "Where",
        "Motive-Impact" to "Why",

        "Task-Incident" to "Resolves",
        "Outcome-Incident" to "Outcome_Of",
        "Impact-Incident" to "Impact_Of",
//        "Description-Entity" to "Description",
//        "Description-Method" to "Description",

        "Location-Wind" to "Where",
        "DateTime-Wind" to "When",
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
        "DateTime", "Description"
    )

    enum class PropertyNames(val key: String) {
        WHO("Entity"),
        WHEN("DateTime"),
        WHERE("Location"),
        WHY("Motive"),
        HOW("Method"),
        TASK("Task"),
        INCIDENT("Incident"),
        OUTCOME("Outcome"),
        IMPACT("Impact"),
        WIND("Wind")
    }

    fun propertyNameFromKey(key: String): PropertyNames? {
        return PropertyNames.entries.find { it.key.equals(key, ignoreCase = true) }
    }

    val SchemaPosTags = listOf("NN", "NNS", "NNP", "NNPS", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ")
    val SchemaNounTags = listOf("NN", "NNS", "NNP", "NNPS")
    val SchemaVerbTags = listOf("VB", "VBD", "VBG", "VBN", "VBP", "VBZ")

    enum class DictionaryTypes {
        NOUN, VERB, SUSPICIOUS
    }
}