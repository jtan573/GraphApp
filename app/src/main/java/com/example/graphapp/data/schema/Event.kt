package com.example.graphapp.data.schema

data class Event(val fields: Map<String, String>)

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object NavigateBack : UiEvent()
}

enum class ActiveButton { NONE, EVENT, FILL, FIND }