package com.example.graphapp.backend.core

object GraphSchema {
    val SchemaEdgeLabels = mapOf(
        "PEOPLEINVOLVED-TASK" to "WHO",
        "APPROACH-TASK" to "HOW",
        "DATETIME-TASK" to "WHEN",
        "LOCATION-TASK" to "WHERE",
        "OBJECTIVE-TASK" to "WHY",

        "PEOPLEINVOLVED-INCIDENT" to "WHO",
        "APPROACH-INCIDENT" to "HOW",
        "DATETIME-INCIDENT" to "WHEN",
        "LOCATION-INCIDENT" to "WHERE",
        "OBJECTIVE-INCIDENT" to "WHY",

        "PEOPLEINVOLVED-OUTCOME" to "WHO",
        "APPROACH-OUTCOME" to "HOW",
        "DATETIME-OUTCOME" to "WHEN",
        "LOCATION-OUTCOME" to "WHERE",
        "OBJECTIVE-OUTCOME" to "WHY",

        "PEOPLEINVOLVED-IMPACT" to "WHO",
        "APPROACH-IMPACT" to "HOW",
        "DATETIME-IMPACT" to "WHEN",
        "LOCATION-IMPACT" to "WHERE",
        "OBJECTIVE-IMPACT" to "WHY",

        "TASK-INCIDENT" to "Resolves",
        "OUTCOME-INCIDENT" to "Outcome_Of",
        "IMPACT-INCIDENT" to "Impact_Of",
//        "Description-PEOPLEINVOLVED" to "Description",
//        "Description-APPROACH" to "Description",

        "LOCATION-WIND" to "WHERE",
        "DATETIME-WIND" to "WHEN",
    )

    val SchemaKeyNodes = listOf(
        "TASK", "INCIDENT", "OUTCOME", "IMPACT"
    )

    val SchemaPropertyNodes = listOf(
        "PEOPLEINVOLVED", "APPROACH", "LOCATION", "OBJECTIVE", "DATETIME"
    )

    val SchemaSemanticPropertyNodes = listOf(
        "PEOPLEINVOLVED", "APPROACH", "OBJECTIVE", "TASK", "INCIDENT", "OUTCOME", "IMPACT"
    )

    val SchemaComputedPropertyNodes = listOf(
        "LOCATION", "DATETIME"
    )

    val SchemaOtherNodes = listOf(
        "DESCRIPTION"
    )

    enum class SchemaEventTypeNames(val key: String) {
        WHO("PEOPLEINVOLVED"),
        WHEN("DATETIME"),
        WHERE("LOCATION"),
        WHY("OBJECTIVE"),
        HOW("APPROACH"),
        TASK("TASK"),
        INCIDENT("INCIDENT"),
        OUTCOME("OUTCOME"),
        IMPACT("IMPACT"),
        DESCRIPTION("DESCRIPTION"),
        WIND("WIND");

        companion object {
            fun fromKey(key: String): SchemaEventTypeNames? =
                entries.find { it.key.equals(key, ignoreCase = true) }

            fun toKey(type: SchemaEventTypeNames): String = type.key
        }
    }

    enum class SchemaKeyEventTypeNames(val key: String) {
        INCIDENT("INCIDENT"),
        TASK("TASK"),
        OUTCOME("OUTCOME"),
        IMPACT("IMPACT");

        companion object {
            fun fromKey(key: String): SchemaKeyEventTypeNames? =
                entries.find { it.key.equals(key, ignoreCase = true) }

            fun toKey(type: SchemaKeyEventTypeNames): String = type.key
        }
    }


    val SchemaPosTags = listOf("NN", "NNS", "NNP", "NNPS", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ")

}