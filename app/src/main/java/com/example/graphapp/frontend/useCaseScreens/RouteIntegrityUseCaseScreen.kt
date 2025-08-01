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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.graphapp.frontend.components.QueryResultCard
import com.example.graphapp.frontend.components.eventForms.RouteForm
import com.example.graphapp.frontend.navigation.NavItem
import com.example.graphapp.frontend.viewmodels.GraphViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RouteIntegrityUseCaseScreen(
    viewModel: GraphViewModel,
    navController: NavController
) {

    val queryResults by viewModel.queryResults.collectAsState()
    val eventAdded by viewModel.createdEvent.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var showForm by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showPlanRoute by remember { mutableStateOf(false) }

    // Data for showcase purposes
    val testData = viewModel.getDataForSuspiciousBehaviourUseCase(listOf<String>(
        "Bombing at Urban Supply Depot",
        "Sniper Nest Detected on Ridge",
        "Sabotage at Communications Relay",
        "Enemy Encampment Spotted in Jungle"
    ))
    val locationList = remember { mutableStateListOf(
        "1.3500,103.8200",
        "1.3489,103.8225",
        "1.3473,103.8246",
        "1.3451,103.8263",
        "1.3426,103.8273",
        "1.3400,103.8282",
        "1.3374,103.8289",
        "1.3349,103.8299",
        "1.3322,103.8304",
        "1.3295,103.8304",
        "1.3269,103.8298",
        "1.3244,103.8287",
        "1.3224,103.8268",
        "1.3204,103.8250",
        "1.3189,103.8228",
        "1.3178,103.8203",
        "1.3164,103.8180",
        "1.3152,103.8156",
        "1.3135,103.8135",
        "1.3120,103.8113",
        "1.3102,103.8093",
        "1.3085,103.8071",
        "1.3074,103.8047",
        "1.3064,103.8022",
        "1.3046,103.8002",
        "1.3033,103.7978",
        "1.3026,103.7952",
        "1.3025,103.7925",
        "1.3021,103.7898",
        "1.3015,103.7872",
        "1.3011,103.7845",
        "1.3002,103.7820",
        "1.3001,103.7793",
        "1.2996,103.7766",
        "1.2988,103.7741",
        "1.2986,103.7714",
        "1.2978,103.7688",
        "1.2976,103.7661",
        "1.2971,103.7635",
        "1.2966,103.7608",
        "1.2965,103.7581",
        "1.2967,103.7554",
        "1.2974,103.7528",
        "1.2977,103.7501",
        "1.2977,103.7474",
        "1.2977,103.7447",
        "1.2979,103.7421",
        "1.2973,103.7394",
        "1.2974,103.7367",
        "1.2969,103.7341"
    ) }

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
                text = "Operational Route Integrity",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
            )
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.DarkGray,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp).padding(4.dp),
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
                    RouteForm (
                        locationList = locationList,
                        onQuery = {
                            coroutineScope.launch {
                                withContext(Dispatchers.Main) {
                                    isLoading = true
                                }
                                viewModel.findAffectedRouteStationsByLocation(locationList)
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    showForm = false //
                                }
                            }
                        },
                        onCancel = { showForm = false }
                    )
                }
                queryResults?.let { QueryResultCard(eventAdded, it, navController) }
                queryResults?.let { showPlanRoute = true }

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
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showForm = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text("+ Route")
                    }

                    if (showPlanRoute) {
                        Button(
                            onClick = { navController.navigate(NavItem.RouteIntegrityImage.route) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("View Map")
                        }
                    }
                }
            }
        }
    }
}