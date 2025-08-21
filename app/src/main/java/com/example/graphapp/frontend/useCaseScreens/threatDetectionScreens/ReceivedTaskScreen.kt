package com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens

import androidx.compose.foundation.layout.Row
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.frontend.navigation.NavItem
import com.example.graphapp.frontend.viewmodels.GraphViewModel
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun ReceivedTaskScreen(viewModel: GraphViewModel,
                       navController: NavController) {

    val threatAlertResults by viewModel.threatAlertResults.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.findThreatAlertAndResponse(
            incidentInputMap = mapOf(
                SchemaEventTypeNames.INCIDENT to "Mid-flight drone propeller failure",
                SchemaEventTypeNames.WHEN to "1723897200000",
                SchemaEventTypeNames.WHERE to "1.3901,103.8072",
                SchemaEventTypeNames.HOW to "Propeller blade sheared mid-flight due to material fatigue, causing crash into storage tent"
            ),
            taskInputMap = mapOf(
                SchemaEventTypeNames.TASK to "Clear Launch Pad and Inspect Fleet",
                SchemaEventTypeNames.WHY to "Prevent launch delays and rule out drone batch-wide mechanical faults"
            )
        )
    }

    val taskInfo = mapOf<String, String>(
        "Task" to "Clear Launch Pad and Inspect Fleet",
        "Objective" to "Prevent launch delays and rule out drone batch-wide mechanical faults",
    )

    val incidentInfo = mapOf<String, String>(
        "Incident" to "Mid-flight drone propeller failure",
        "Details" to "Propeller blade sheared mid-flight due to material fatigue, causing crash into storage tent",
        "DateTime" to "2025-01-18T13:20Z",
        "Location" to "1.3901,103.8072 (150m away from you)",
    )

    val testImpacts = listOf(
        "Mission delay risk due to debris clearance",
        "Potential fleet-wide mechanical fault from dust ingress",
        "Safety risk for personnel near crash site",
    )

    val scrollState = rememberScrollState()
    val initiallyExpanded = false
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {

        Text(
            text = "Incoming Task",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 64.dp, bottom = 12.dp, start = 10.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent // light red background
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(3.dp, Color(0xFF9C6440))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Incident reported:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    incidentInfo.forEach { (label, value) ->
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(
                                text = "$label:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E4E8C)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF99CCD1) // no background
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "TASK ASSIGNED",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    taskInfo.forEach { (label, value) ->
                        Column(
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "$label:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E4E8C)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }
                    }
                    Button(
                        onClick = { navController.navigate(NavItem.TaskInstructionsScreen.route) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF45A0A8),          // background color
                            contentColor = Color.White
                        )
                    ) {
                        threatAlertResults?.let {
                            Text("Instructions on How to Perform", fontSize = 14.sp)
                        } ?: Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 3.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Looking for instructions...")
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(Modifier.animateContentSize()) {
                    threatAlertResults?.let {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Predicted Impacts",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                modifier = Modifier.rotate(if (expanded) 180f else 0f)
                            )
                        }

                        HorizontalDivider()

                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                testImpacts.forEachIndexed { i, impact ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // bullet dot
                                        Text("•  ", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            impact,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (i != testImpacts.lastIndex) {
                                        Spacer(Modifier.height(8.dp))
                                        HorizontalDivider(color = Color.LightGray)
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                Button(
                                    onClick = { navController.navigate(NavItem.ThreatAnalysisScreen.route) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF596569),          // background color
                                        contentColor = Color.White                   // text/icon color
                                    )
                                ) {
                                    Text("Results Analysis >", fontSize = 14.sp)
                                }
                            }
                        }
                    }?: Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Analysing for potential impacts...")
                    }
                }
            }
        }
    }
}