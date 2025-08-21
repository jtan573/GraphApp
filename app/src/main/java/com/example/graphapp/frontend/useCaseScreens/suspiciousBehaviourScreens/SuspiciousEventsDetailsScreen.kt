package com.example.graphapp.frontend.useCaseScreens.suspiciousBehaviourScreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.rememberNavController
import com.example.graphapp.R
import com.example.graphapp.backend.core.GraphSchema.SchemaEventTypeNames
import com.example.graphapp.frontend.useCaseScreens.formatMillisToSGT
import com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens.ReceivedTaskScreen
import com.example.graphapp.frontend.viewmodels.GraphViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SuspiciousEventsDetailsScreen(
    viewModel: GraphViewModel
) {

//     Data for showcase purposes
    val previewIncidents = viewModel.getDataForSuspiciousBehaviourUseCase(listOf<String>(
        "Subject loiters near restricted zone appearing to scan the area",
        "Subject spotted wandering aimlessly along perimeter fencing",
        "Person discreetly writing or sketching near checkpoint structure",
        "Unusual handoff of item occurs at public bench with minimal interaction",
        "Small group appears to annotate or inspect public utility fixture"
    ))

    // Dummy preview data
//    val previewIncidents = listOf(
//        mapOf(
//            "WHEN" to "1694354700000", // 10 Sep 2023, 16:45 UTC+8
//            "INCIDENT" to "Subject loiters near restricted zone appearing to scan the area"
//        ),
//        mapOf(
//            "WHEN" to "1694446320000", // 11 Sep 2023, 18:12 UTC+8
//            "INCIDENT" to "Subject spotted wandering aimlessly along perimeter fencing"
//        ),
//        mapOf(
//            "WHEN" to "1694514300000", // 12 Sep 2023, 15:05 UTC+8
//            "INCIDENT" to "Person discreetly writing or sketching near checkpoint structure"
//        ),
//        mapOf(
//            "WHEN" to "1694615820000", // 13 Sep 2023, 21:37 UTC+8
//            "INCIDENT" to "Unusual handoff of item occurs at public bench with minimal interaction"
//        ),
//        mapOf(
//            "WHEN" to "1694702100000", // 14 Sep 2023, 17:55 UTC+8
//            "INCIDENT" to "Small group appears to annotate or inspect public utility fixture"
//        )
//    )
    val sortedIncidents = previewIncidents.sortedBy { it[SchemaEventTypeNames.WHEN.key] }


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
                text = "Task Details",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp).padding(bottom = 8.dp)
        ) {
            item {
                Text(
                    text = "Also inspect the following incidents:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Image(
                    painter = painterResource(id = R.drawable.all_suspicious_events_img),
                    contentDescription = "All suspicious events",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentScale = ContentScale.FillWidth
                )
            }

            // Table header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Date/Time", fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(text = "Incident", fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(2f), textAlign = TextAlign.Center)
                }
                HorizontalDivider(color = Color.Gray, thickness = 1.dp)
            }

            // Table rows
            items (sortedIncidents) { incident ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMillisToSGT(incident[SchemaEventTypeNames.WHEN.key]),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = incident[SchemaEventTypeNames.INCIDENT.key] ?: "-",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(2f)
                    )
                }
                HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
            }
        }
    }
}

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun IncidentTablePreview() {
//    SuspiciousEventsDetailsScreen()
//}