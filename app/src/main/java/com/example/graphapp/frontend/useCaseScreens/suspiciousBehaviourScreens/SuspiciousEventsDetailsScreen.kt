package com.example.graphapp.frontend.useCaseScreens.suspiciousBehaviourScreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graphapp.R
import com.example.graphapp.frontend.components.IncidentTagTable
import com.example.graphapp.frontend.viewmodels.GraphViewModel

@Composable
fun SuspiciousEventsDetailsScreen(
    viewModel: GraphViewModel
) {
    val suspiciousDetectionResults by viewModel.suspiciousDetectionResults.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Task Details",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
        ) {
            item {
                Text(
                    text = "Potential Related Incidents:",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Image(
                    painter = painterResource(id = R.drawable.suspicious_event_details),
                    contentDescription = "All suspicious events",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentScale = ContentScale.FillWidth
                )
            }

            item {
                Text(
                    text = "Details",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Table rows
            suspiciousDetectionResults?.similarIncidents?.sortedByDescending { it.simScore }
                ?.forEach { incident ->
                    item {
                        Card() {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                Text(
                                    text = "Incident: ${incident.eventName}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                IncidentTagTable(incident)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
        }
    }
}
