package com.example.graphapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.graphapp.data.schema.QueryResult

@Composable
fun QueryResultCard(
    eventAdded: String,
    queryResults: QueryResult,
) {
    when (queryResults) {
        is QueryResult.IncidentResponse ->
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = eventAdded,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp),
                    color = Color(0xFF2E4E8C)
                )
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
                            modifier = Modifier.padding(top = 8.dp).padding(horizontal = 10.dp)
                        )
                        Text(
                            text = user.specialisation,
                            modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.DarkGray,
                        )
                    }
                }
            }
    }
}
