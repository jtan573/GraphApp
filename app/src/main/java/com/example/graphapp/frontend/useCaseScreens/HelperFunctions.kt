package com.example.graphapp.frontend.useCaseScreens

import java.text.SimpleDateFormat

import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatMillisToSGT(millis: String?): String {
    return try {
        val epochMillis = millis?.toLong() ?: return "-"
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("GMT+8") // UTC+8
        sdf.format(Date(epochMillis))
    } catch (e: Exception) {
        "-"
    }
}