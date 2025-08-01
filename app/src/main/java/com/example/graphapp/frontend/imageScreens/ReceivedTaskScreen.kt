package com.example.graphapp.frontend.imageScreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun ReceivedTaskScreen() {

    val taskInfo = mapOf<String, String>(
        "Task" to "Clear Launch Pad and Inspect Fleet",
        "Motive" to "Prevent launch delays and rule out drone batch-wide mechanical faults",
        "Method" to "Clear debris from lift pad and conduct rotor health scan across nearby UAVs"
    )

    val incidentInfo = mapOf<String, String>(
        "Incident" to "Drone Rotor Jammed During Lift-Off",
        "How" to "Dust ingress in rotor hub stalled motor mid-ascent, causing drone to crash near fire truck",
        "DateTime" to "2025-01-18T13:20Z",
        "Location" to "1.3012,103.7880",
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Task",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 64.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)
        ) {
            Text(
                text = "Task assigned:",
                modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
            )
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

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                color = Color.Gray,
                thickness = 1.dp
            )

            Text(
                text = "Incident reported near you:",
                modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
            )
            incidentInfo.forEach { (label, value) ->
                Column (modifier = Modifier.padding(bottom = 5.dp)) {
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
}