package com.example.graphapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graphapp.ui.components.GraphWebView
import com.example.graphapp.ui.viewmodels.GraphViewModel

@Composable
fun PersonnelScreen(
    viewModel: GraphViewModel
) {
    val graphJson by viewModel.userGraphData.collectAsState()
    val selectedFilter = "All"
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Personnel Tracker",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 64.dp, bottom = 8.dp),
            )
        }

        if (graphJson != null) {
            GraphWebView(
                graphJson = graphJson,
                selectedFilter = selectedFilter,
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                htmlFileName = "userGraph.html"
            )
        } else {
            Text(
                text = "Loading graph...",
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}