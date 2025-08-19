package com.example.graphapp.frontend.useCaseScreens.threatDetectionScreens

import android.text.Layout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.graphapp.backend.schema.ActiveButton
import com.example.graphapp.frontend.navigation.NavItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun ReceivedTaskScreen(navController: NavController) {

    val taskInfo = mapOf<String, String>(
        "Task" to "Clear Launch Pad and Inspect Fleet",
        "Objective" to "Prevent launch delays and rule out drone batch-wide mechanical faults",
//        "Method" to "Clear debris from lift pad and conduct rotor health scan across nearby UAVs"
    )

    val incidentInfo = mapOf<String, String>(
        "Incident" to "Drone Rotor Jammed During Lift-Off",
        "Details" to "Dust ingress in rotor hub stalled motor mid-ascent, causing drone to crash near fire truck",
        "DateTime" to "2025-01-18T13:20Z",
        "Location" to "1.3012,103.7880 (150m away from you)",
    )

    val scrollState = rememberScrollState()

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
                    containerColor = Color(0xFFFFB9B9) // light red background
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    containerColor = Color.Transparent // no background
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // optional, no shadow
                border = BorderStroke(2.dp, Color(0xFF48C7D4)) // border color + thickness
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
                            contentColor = Color.White                   // text/icon color
                        )
                    ) {
                        Text("Instructions on How to Perform", fontSize = 12.sp)
                    }
                }
            }
        }

    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ReceivedTaskScreenPreview() {
    val dummyNavController = rememberNavController()
    ReceivedTaskScreen(dummyNavController)
}