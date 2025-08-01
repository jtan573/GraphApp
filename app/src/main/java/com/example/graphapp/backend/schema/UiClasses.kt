package com.example.graphapp.backend.schema

import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.db.UserNodeEntity

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object NavigateBack : UiEvent()
}

enum class ActiveButton { NONE, EVENT, FILL, FIND }
