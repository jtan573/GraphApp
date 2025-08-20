package com.example.graphapp.frontend.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.graphapp.backend.schema.UiEvent
import com.example.graphapp.frontend.components.PersonnelSearchForm
import com.example.graphapp.frontend.components.GraphWebView
import com.example.graphapp.frontend.navigation.NavItem
import com.example.graphapp.frontend.viewmodels.GraphViewModel
import kotlinx.coroutines.launch

@Composable
fun PersonnelScreen(
    viewModel: GraphViewModel,
    navController: NavController
) {
    val graphJson by viewModel.userGraphData.collectAsState()
    val activeUsers by viewModel.allActiveUsers.collectAsState()
    val selectedFilter = "All"
    var viewMode by remember { mutableStateOf("list") } // or "graph"

    val snackbarHostState = remember { SnackbarHostState() }
    val uiEventFlow = viewModel.uiEvent

    LaunchedEffect(Unit) {
        viewModel.createFullPersonnelGraph()
        uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                UiEvent.NavigateBack -> {
                    // TODO: handle navigation here
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp).padding(bottom = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Personnel Tracker",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 64.dp),
                )
            }

            if (viewMode == "graph") {
                if (graphJson != null) {
                    GraphWebView(
                        graphJson = graphJson,
                        selectedFilter = selectedFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
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
            } else {
                Text(
                    text = "List Of Active Users:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(
                    userScrollEnabled = true
                ) {
                    items(activeUsers) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = "${user.identifier}: ${user.role}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .padding(horizontal = 10.dp)
                            )
                            Text(
                                text = user.specialisation,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .padding(horizontal = 10.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.DarkGray,
                            )
                        }
                    }
                }

            }

        }
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Button (
                onClick = { viewMode = "list" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewMode == "list") MaterialTheme.colorScheme.primary else Color.Gray
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(70.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("List")
            }
            Button (
                onClick = { viewMode = "graph" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewMode == "graph") MaterialTheme.colorScheme.primary else Color.Gray
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.width(70.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Graph")
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}