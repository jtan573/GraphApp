package com.example.graphapp.frontend.components

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object NavigateBack : UiEvent()
}

enum class ActiveButton { NONE, EVENT, FILL, FIND }
