package com.example.graphapp.frontend.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GraphViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GraphViewModel::class.java)) {
            return GraphViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
