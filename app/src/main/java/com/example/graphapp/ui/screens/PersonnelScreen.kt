package com.example.graphapp.ui.screens

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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.simulateHotReload
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.graphapp.data.schema.UiEvent
import com.example.graphapp.ui.components.PersonnelSearchForm
import com.example.graphapp.ui.components.GraphWebView
import com.example.graphapp.ui.navigation.NavItem
import com.example.graphapp.ui.viewmodels.GraphViewModel
import kotlinx.coroutines.launch

@Composable
fun PersonnelScreen(
    viewModel: GraphViewModel,
    navController: NavController
) {
    val graphJson by viewModel.userGraphData.collectAsState()
    val contactState by viewModel.relevantContactState.collectAsState()
    val activeUsers by viewModel.allActiveUsers.collectAsState()
    val selectedFilter = "All"

    val coroutineScope = rememberCoroutineScope()
    var showForm by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var localUserQueryDescription by remember { mutableStateOf("") }
    var localUserQueryLocation by remember { mutableStateOf("1.3580,103.6900") }
    var viewMode by remember { mutableStateOf("list") } // or "graph"

    val snackbarHostState = remember { SnackbarHostState() }
    val uiEventFlow = viewModel.uiEvent

    LaunchedEffect(Unit) {
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
                Button(
                    onClick = { showForm = !showForm },
                    enabled = !isLoading,
                    modifier = Modifier.padding(end = 3.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (showForm) "Hide" else "Search",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showForm) {
                PersonnelSearchForm(
                    description = localUserQueryDescription,
                    onDescriptionChange = { localUserQueryDescription = it },
                    location = localUserQueryLocation,
                    onLocationChange = { localUserQueryLocation = it },
                    onSubmit = {
                        coroutineScope.launch {
                            isLoading = true
                            viewModel.findRelevantPersonnelOnDemand(
                                eventLocation = localUserQueryLocation,
                                eventDescription = localUserQueryDescription
                            )
                            isLoading = false
                            showForm = false
                        }
                    }
                )
            }

            contactState?.let {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF95b0db))
                        .padding(16.dp)
                ) {
                    val queryString = if (localUserQueryDescription == "") {
                        "No input description."
                    } else { localUserQueryDescription }
                    Text(
                        text = "Query: $queryString",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "User's Location: $localUserQueryLocation",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (contactState!!.isEmpty()) {
                        Text(
                            text = "No active personnel within 3000m radius is found.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .padding(horizontal = 10.dp)
                        )
                    } else {
                        Text(
                            text = "Nearby Active Users Found:",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        contactState!!.forEach { (user, distance) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Text(
                                    text = "(${distance}m away) ${user.identifier}: ${user.role}",
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
                    Row (
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { navController.navigate(NavItem.PersonnelMapImage.route) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Show Map")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
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