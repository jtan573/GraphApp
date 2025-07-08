package com.example.graphapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.graphapp.data.local.VectorDatabase
import com.example.graphapp.ui.screens.MainScreen
import com.example.graphapp.ui.theme.GraphAppTheme
import com.example.graphapp.ui.viewmodels.GraphViewModel
import com.example.graphapp.ui.viewmodels.GraphViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: GraphViewModel by viewModels() {
        GraphViewModelFactory(application)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VectorDatabase.init(this)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            GraphAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel, navController = navController)
                }
            }
        }
    }
}


