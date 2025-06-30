package com.example.graphapp.data.schema

object GraphSchema {
    val edgeLabels = mapOf(
//        "User-App" to "Who",
//        "App-DeviceType" to "How",
//        "App-Duration" to "When",
//        "App-Country" to "Where",
//        "App-Category" to "Why"
        "Article-Entity" to "Who",
        "Article-Method" to "How",
        "Article-Date" to "When",
        "Article-Location" to "Where",
        "Article-Motive" to "Why"
    )

    val keyNodes = listOf(
//        "App",
//        "User",
        "Article"
    )

    val propertyNodes = listOf(
//        "Category",
//        "Country",
//        "Duration",
//        "DeviceType"
        "Entity",
        "Method",
        "Date",
        "Location",
        "Motive"
    )
}