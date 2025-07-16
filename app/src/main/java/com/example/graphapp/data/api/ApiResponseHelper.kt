package com.example.graphapp.data.api

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun buildApiResponseFromResult(result: Any): ApiResponse {

    val responseData = when (result) {
        is PredictMissingPropertiesResponse -> ResponseData.PredictMissingPropertiesData(result)
        is ProvideRecommendationsResponse -> ResponseData.ProvideRecommendationsData(result)
        is PatternFindingResponse -> ResponseData.PatternFindingData(result)
        is DiscoverEventsResponse -> ResponseData.DiscoverEventsData(result)
        is ReplicaDetectionResponse -> ResponseData.DetectReplicaEventData(result)
        else -> throw IllegalArgumentException("Unsupported response type: ${result::class}")
    }

    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val timestamp = sdf.format(Date())

    val apiRes = ApiResponse(
        status = "success",
        timestamp = timestamp,
        data = responseData
    )
    Log.d("API RESPONSE", "Response: $apiRes")

    return apiRes
}

