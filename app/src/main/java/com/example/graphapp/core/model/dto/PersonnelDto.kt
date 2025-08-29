package com.example.graphapp.core.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDetailData (
    val identifier: String? = null,
    val role: String? = null,
    val specialisation: String? = null,
    val currentLocation: String? = null,
)