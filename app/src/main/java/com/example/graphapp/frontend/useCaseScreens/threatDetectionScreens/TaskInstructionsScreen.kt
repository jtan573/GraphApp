package com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graphapp.frontend.navigation.NavItem

@Composable
fun TaskInstructionsScreen() {
    val taskInfo = mapOf<String, String>(
        "Task" to "Clear Launch Pad and Inspect Fleet",
        "Objective" to "Prevent launch delays and rule out drone batch-wide mechanical faults",
//        "Method" to "Clear debris from lift pad and conduct rotor health scan across nearby UAVs"
    )
    val taskInstructions = listOf<String>(
        "Suspend UAV operations and power down launch pad.",
        "Assemble crew and ensure PPE is worn.",
        "Clear debris and hazards from the pad surface.",
        "Inspect UAVs for cracks, loose fittings, or damage.",
        "Check rotor assemblies, connectors, and landing gear.",
        "Log inspection results and flag anomalies.",
        "Report summary to operations officer.",
        "Confirm pad is clear and restore launch readiness."
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {

        Text(
            text = "Task Details",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 64.dp, bottom = 12.dp, start = 10.dp),
        )

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF9DCFD4)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // optional, no shadow
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

                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),// light gray background
                border = BorderStroke(2.dp, Color(0xFF48C7D4)) // border color + thickness
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Task Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    taskInstructions.forEachIndexed { index, instruction ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E4E8C),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = instruction,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            Card(
                onClick = {  },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFB9B9) // light red background
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "RESULTS ANALYSIS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Arrow Right",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Preview (showBackground = true, showSystemUi = true)
@Composable
fun TaskInstructionsPreview() {
    TaskInstructionsScreen()
}