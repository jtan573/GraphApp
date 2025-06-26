package com.example.graphapp.data.schema

object GraphSchema {
    val edgeLabels = mapOf(
        "User-App" to "Who",
        "App-DeviceType" to "How",
        "App-Duration" to "When",
        "App-Country" to "Where",
        "App-Category" to "Why"
    )

    val keyNodes = listOf(
        "App",
        "User"
    )

    val propertyNodes = listOf(
        "Category",
        "Country",
        "Duration",
        "DeviceType"
    )
}