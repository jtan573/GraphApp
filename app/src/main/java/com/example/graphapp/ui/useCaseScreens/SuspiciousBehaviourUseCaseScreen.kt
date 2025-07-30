package com.example.graphapp.ui.useCaseScreens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.graphapp.ui.components.QueryResultCard
import com.example.graphapp.ui.components.eventForms.IncidentForm
import com.example.graphapp.ui.viewmodels.GraphViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.set

@Composable
fun SuspiciousBehaviourUseCaseScreen(viewModel: GraphViewModel, navController: NavController) {

    val queryResults by viewModel.queryResults.collectAsState()
    val eventAdded by viewModel.createdEvent.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var showForm by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var threatFieldKeys = listOf<String>("Entity", "Method", "Location", "Motive", "Date", "Description")

    val eventInputMap = remember(threatFieldKeys) {
        mutableStateMapOf<String, String>().apply {
            threatFieldKeys.forEach { putIfAbsent(it, "") }
        }
    }

    // Data for showcase purposes
    val testData = viewModel.getDataForSuspiciousBehaviourUseCase(listOf<String>(
        "Subject loiters near restricted zone appearing to scan the area",
        "Subject spotted wandering aimlessly along perimeter fencing",
        "Person discreetly writing or sketching near checkpoint structure",
        "Unusual handoff of item occurs at public bench with minimal interaction",
        "Small group appears to annotate or inspect public utility fixture"
    ))

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Suspicious Behaviour Detection",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Show form only if visible
                AnimatedVisibility(visible = showForm) {
                    IncidentForm (
                        fieldKeys = threatFieldKeys,
                        eventInputMap = eventInputMap,
                        onQuery = {
                            coroutineScope.launch {
                                withContext(Dispatchers.Main) {
                                    isLoading = true
                                }
                                viewModel.findDataIndicatingSuspiciousBehaviour(eventInputMap)
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    eventInputMap.clear()
                                    threatFieldKeys.forEach { eventInputMap[it] = "" }
                                    showForm = false //
                                }
                            }
                        },
                        onCancel = { showForm = false }
                    )
                }
                queryResults?.let { QueryResultCard(eventAdded, it, navController) }

                Text(
                    text = "List Of Incidents Reported:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                testData.forEach { incident ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = "Incident: ${incident["Incident"]}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(top = 8.dp).padding(horizontal = 10.dp)
                        )
                        Text(
                            text = "How: ${incident["Method"]}",
                            modifier = Modifier.padding(horizontal = 10.dp).padding(top = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                        )
                        Text(
                            text = "Observed on: ${incident["Date"]}",
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                        )
                        Text(
                            text = "Location: ${incident["Location"]}",
                            modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                        )
                    }
                }
            }

            if (!showForm) {
                Button(
                    onClick = { showForm = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),

                    ) {
                    Text("+ Incident")
                }
            }
        }
    }
}