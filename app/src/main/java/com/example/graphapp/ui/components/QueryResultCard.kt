package com.example.graphapp.ui.components

import android.R
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.graphapp.data.schema.QueryResult

@Composable
fun QueryResultCard(
    eventAdded: Map<String, String>,
    queryResults: QueryResult,
) {
    Column(modifier = Modifier
        .padding(top = 16.dp)
        .padding(horizontal = 16.dp)
    ) {
        eventAdded.forEach { (inputType, inputString) ->
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
                    Text(
                        text = "Nearby Active Troopers:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
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


                queryResults.similarIncidents?.forEach { (type, recList) ->
                    Text(
                        text = "Similar Events (by $type)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
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
                                modifier = Modifier.padding(horizontal = 10.dp).padding(top = 8.dp)
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
                                modifier = Modifier.padding(horizontal = 10.dp).padding(bottom = 2.dp)
                            )
                            Text(
                                text = "How: ${event.eventProperties["Method"]}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = FontStyle.Italic
                                ),
                                color = Color.DarkGray,
                                modifier = Modifier.padding(horizontal = 10.dp).padding(bottom = 8.dp)
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

                    queryResults.incidentsAffectingStations.forEach { (stationDetails, incidentsList) ->
                        Text(
                            text = "Incidents near Stop ${stationDetails.first+1}: ${stationDetails.second}:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 5.dp),
                        )
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
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                        .padding(top = 8.dp)
                                )
                                Text(
                                    text = "Location: ${incident.eventProperties["Location"]}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = FontStyle.Italic
                                    ),
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                                incident.eventProperties["Date"]?.let {
                                    Text(
                                        text = "Observed on: $it",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontStyle = FontStyle.Italic
                                        ),
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(horizontal = 10.dp)
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
                                        modifier = Modifier.padding(horizontal = 10.dp)
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
