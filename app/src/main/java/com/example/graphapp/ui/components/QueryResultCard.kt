package com.example.graphapp.ui.components

import android.R
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.graphapp.data.schema.QueryResult
import com.example.graphapp.ui.navigation.NavItem

@Composable
fun QueryResultCard(
    eventAdded: Map<String, String>,
    queryResults: QueryResult,
    navController: NavController
) {
    Column(modifier = Modifier
        .padding(top = 10.dp)
        .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Incident: ${eventAdded["Incident"]}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black
            ),
            modifier = Modifier.padding(bottom = 5.dp),
            color = Color(0xFF2E4E8C)
        )
        eventAdded.forEach { (inputType, inputString) ->
            if (inputType == "Incident") return@forEach
            Text(
                text = "$inputType: $inputString",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 5.dp),
                color = Color(0xFF2E4E8C)
            )
        }
    }

    when (queryResults) {
        is QueryResult.IncidentResponse -> {
            Column(modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF95b0db))
                .padding(16.dp)
            ) {
                if (queryResults.nearbyActiveUsersMap != null) {
                    Row (
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "Nearby Active Troopers:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                        Button(
                            onClick = { navController.navigate(NavItem.AlertTroopersScreen.route) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Send Alert")
                        }
                    }
                    queryResults.nearbyActiveUsersMap.forEach { (user, distance) ->
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
                    Spacer(modifier = Modifier.padding(vertical = 12.dp))
                }


                if (queryResults.potentialImpacts != null) {
                    Text(
                        text = "Potential impacts of the incident:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )

                    queryResults.potentialImpacts.forEach { recommendation ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = recommendation,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.padding(vertical = 12.dp))
                }

                if (queryResults.potentialTasks != null) {
                    Text(
                        text = "Potential Tasks of the incident:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )

                    queryResults.potentialTasks.forEach { task ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = task,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.padding(vertical = 12.dp))
                }

                if (queryResults.taskAssignment != null) {
                    Text(
                        text = "Task Assignment:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )

                    queryResults.taskAssignment.forEach { (task, troopers) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = task,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                            troopers.forEach { trooper ->
                                Text(
                                    text = "${trooper.identifier}: ${trooper.role}",
                                    modifier = Modifier
                                        .padding(bottom = 3.dp)
                                        .padding(horizontal = 10.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.DarkGray,
                                )
                                Text(
                                    text = trooper.specialisation,
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .padding(horizontal = 10.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.DarkGray,
                                )
                            }
                            Row (
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { navController.navigate(NavItem.AssignTaskScreen.route) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text("Assign Task")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.padding(vertical = 12.dp))
                }


                queryResults.similarIncidents?.forEach { (type, recList) ->
                    Row (
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Similar Events (by $type)",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 5.dp),
                        )
                        Button(
                            onClick = { navController.navigate(NavItem.AssignTaskScreen.route) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Assign Task")
                        }
                    }
                    recList.forEach { event ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = "Incident: ${event.eventName}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .padding(top = 8.dp)
                            )
                            Text(
                                text = "Location: ${event.eventProperties["Location"]}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = Color.DarkGray,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                            Text(
                                text = "Observed on: ${event.eventProperties["Date"]}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = Color.DarkGray,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .padding(bottom = 2.dp)
                            )
                            Text(
                                text = "How: ${event.eventProperties["Method"]}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = Color.DarkGray,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .padding(bottom = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.padding(vertical = 10.dp))
                }

                if (queryResults.incidentsAffectingStations != null) {
                    Text(
                        text = "Incidents potentially affecting route:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )

                    queryResults.incidentsAffectingStations.forEach { (type, incidentsList) ->
                        if (type == "Proximity") {
                            Text(
                                text = "Incidents occurring within 3km radius of route:",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 5.dp),
                            )
                        } else {
                            Text(
                                text = "Incidents further away that may disrupt route due to Wind",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 5.dp),
                            )
                        }
                        incidentsList.forEach { incident ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Text(
                                    text = "Incident: ${incident.eventName}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp)
                                        .padding(top = 8.dp)
                                )
                                Text(
                                    text = "Location: ${incident.eventProperties["Location"]}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = FontStyle.Italic
                                    ),
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 2.dp
                                    )
                                )
                                incident.eventProperties["Date"]?.let {
                                    Text(
                                        text = "Observed on: $it",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontStyle = FontStyle.Italic
                                        ),
                                        color = Color.DarkGray,
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp)
                                            .padding(bottom = 2.dp)
                                    )
                                }
                                incident.eventProperties["Method"]?.let {
                                    Text(
                                        text = "How: $it",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontStyle = FontStyle.Italic
                                        ),
                                        color = Color.DarkGray,
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp)
                                            .padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.padding(vertical = 10.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
