package com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens

import android.media.metrics.Event
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.graphapp.backend.dto.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.backend.schema.SimilarEventTags
import com.example.graphapp.data.api.EventDetails
import com.example.graphapp.data.api.ThreatAlertResponse
import com.example.graphapp.frontend.components.IncidentCardCollapsible
import com.example.graphapp.frontend.viewmodels.GraphViewModel
import kotlin.collections.set

@Composable
fun ThreatAnalysisScreen(
    viewModel: GraphViewModel
) {
    val threatAlertResults by viewModel.threatAlertResults.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.findThreatAlertAndResponse(mapOf(
            SchemaEventTypeNames.INCIDENT.key to "Mid-flight drone propeller failure",
            SchemaEventTypeNames.WHEN.key to "1723897200000",
            SchemaEventTypeNames.WHERE.key to "1.3901,103.8072",
            SchemaEventTypeNames.HOW.key to "Propeller blade sheared mid-flight due to material fatigue, causing crash into storage tent"
        ))
    }

    val incidentInfo = mapOf<String, String>(
        "Incident" to "Drone Rotor Jammed During Lift-Off",
        "Details" to "Dust ingress in rotor hub stalled motor mid-ascent, causing drone to crash near fire truck",
        "DateTime" to "2025-01-18T13:20Z",
        "Location" to "1.3012,103.7880 (150m away from you)",
    )

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Results Analysis",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(3.dp, Color(0xFF9C6440))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Incident reported:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    incidentInfo.forEach { (label, value) ->
                        Column(Modifier.padding(bottom = 8.dp)) {
                            Text(
                                text = "$label:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E4E8C)
                            )
                            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Similar Incidents found in Database:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        item {
            when (val res = threatAlertResults) {
                null -> {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(8.dp))
                        Text("Loading results…")
                    }
                }
                else -> {
                    res.similarIncidents?.let { incident ->
                        IncidentCardCollapsible(
                            incidents = incident,
                            impacts = res.potentialImpacts
                        )
                    } ?: Text("No similar incidents found")
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }
    }
}