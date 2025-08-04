package com.example.graphapp.frontend.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.graphapp.backend.dto.GraphSchema
import com.example.graphapp.backend.dto.GraphSchema.PropertyNames
import com.example.graphapp.data.api.ThreatAlertResponse
import com.example.graphapp.frontend.navigation.NavItem
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.parse

@Composable
fun QueryResultCard(
    eventAdded: Map<String, String>,
    queryResults: ThreatAlertResponse,
    navController: NavController
) {

    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC+08:00")

    Column(modifier = Modifier
        .padding(top = 10.dp)
        .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Incident: ${eventAdded[PropertyNames.INCIDENT.key]}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black
            ),
            modifier = Modifier.padding(bottom = 5.dp),
            color = Color(0xFF2E4E8C)
        )
        eventAdded.forEach { (inputType, inputString) ->
            if (inputType == PropertyNames.INCIDENT.key) return@forEach

            var updatedString: String = inputString
            if (inputType == PropertyNames.WHEN.key) {
                updatedString = sdf.format(Date(inputString.toLong()))
            }
            Text(
                text = "$inputType: $updatedString",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 5.dp),
                color = Color(0xFF2E4E8C)
            )
        }
    }

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
                    onClick = { navController.navigate(NavItem.SuspiciousLocationScreen.route) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("View Detail")
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
                        text = "Location: ${event.eventProperties[PropertyNames.WHERE.key]}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic
                        ),
                        color = Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                    event.eventProperties[PropertyNames.WHEN.key]?.let {
                        Text(
                            text = "Observed on: ${sdf.format(Date(it.toLong()))}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = Color.DarkGray,
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .padding(bottom = 2.dp)
                        )
                    }
                    Text(
                        text = "How: ${event.eventProperties[PropertyNames.HOW.key]}",
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
                            text = "Location: ${incident.eventProperties[PropertyNames.WHERE.key]}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = Color.DarkGray,
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 2.dp
                            )
                        )
                        incident.eventProperties[PropertyNames.WHEN.key]?.let {
                            Text(
                                text = "Observed on: ${sdf.format(Date(it.toLong()))}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = Color.DarkGray,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp)
                                    .padding(bottom = 2.dp)
                            )
                        }
                        incident.eventProperties[PropertyNames.HOW.key]?.let {
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
}
