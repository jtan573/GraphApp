package com.example.graphapp.data.classes

object GraphSchema {
    val relationshipRules = mapOf(
        "User-App" to "Who",
        "App-DeviceType" to "How",
        "App-Duration" to "When",
        "App-Country" to "Where",
        "App-Category" to "Why"
    )
}