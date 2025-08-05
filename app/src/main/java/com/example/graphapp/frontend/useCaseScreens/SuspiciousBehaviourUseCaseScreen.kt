package com.example.graphapp.frontend.useCaseScreens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.graphapp.backend.dto.GraphSchema
import com.example.graphapp.backend.dto.GraphSchema.PropertyNames
import com.example.graphapp.backend.dto.GraphSchema.SchemaOtherNodes
import com.example.graphapp.backend.dto.GraphSchema.SchemaPropertyNodes
import com.example.graphapp.frontend.components.QueryResultCard
import com.example.graphapp.frontend.components.eventForms.IncidentForm
import com.example.graphapp.frontend.viewmodels.GraphViewModel
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
    var threatFieldKeys = SchemaPropertyNodes + SchemaOtherNodes

    val eventInputMap = remember(threatFieldKeys) {
        mutableStateMapOf<String, String>().apply {
            threatFieldKeys.forEach { putIfAbsent(it, "") }
        }
    }

    LaunchedEffect(Unit) {
        eventInputMap[PropertyNames.INCIDENT.key] = "Subject lurking around the area, trying to avoid cameras"
        eventInputMap[PropertyNames.WHERE.key] = "1.3425,103.6897"
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
                text = "Suspicious Events Detection",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
            )
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.DarkGray,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(40.dp).padding(10.dp),
                )
            }
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
                            text = "Incident: ${incident[PropertyNames.INCIDENT.key]}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(top = 8.dp).padding(horizontal = 10.dp)
                        )
                        Text(
                            text = "How: ${incident[PropertyNames.HOW.key]}",
                            modifier = Modifier.padding(horizontal = 10.dp).padding(top = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                        )
                        Text(
                            text = "Observed on: ${incident[PropertyNames.WHEN.key]}",
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                        )
                        Text(
                            text = "Location: ${incident[PropertyNames.WHERE.key]}",
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