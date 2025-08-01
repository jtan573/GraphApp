package com.example.graphapp.frontend.imageScreens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.graphapp.R
import com.example.graphapp.data.db.UserNodeEntity
import com.example.graphapp.frontend.navigation.NavItem

@Composable
fun AssignTaskToTroopersScreen(
    navController: NavController
) {
    val taskInfo = mapOf<String, String>(
        "Task" to "Clear Launch Pad and Inspect Fleet",
        "Motive" to "Prevent launch delays and rule out drone batch-wide mechanical faults",
        "Method" to "Clear debris from lift pad and conduct rotor health scan across nearby UAVs"
    )
    val testData = listOf<UserNodeEntity>(
        UserNodeEntity(
            identifier = "OPS-062",
            role = "UAV Maintenance Crew Lead",
            specialisation = "Oversees launch pad readiness and executes pre-flight mechanical integrity checks on UAV fleets.",
            currentLocation = "1.3863,103.8004"
        ),
        UserNodeEntity(
            identifier = "ENG-074",
            role = "Rotor Diagnostics Technician",
            specialisation = "Performs debris removal and conducts fine-grain diagnostics on rotor assemblies before lift-off.",
            currentLocation = "1.3950,103.8041"
        ),
        UserNodeEntity(
            identifier = "DRN-047",
            role = "Drone Recovery Specialist",
            specialisation = "Locates and retrieves downed UAVs and handles data uplink reinitialisation post-crash.",
            currentLocation = "1.3937,103.8125"
        )
    )
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Relevant Personnel Map",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
                .padding(horizontal = 16.dp),
        )
        Text(
            text = "Task to be assigned:",
            modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF2E4E8C),
        )
        taskInfo.forEach { (label, value) ->
            Text(
                text = "$label: $value",
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2E4E8C),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
        ) {

            Text(
                text = "Active Personnel in the area:",
                modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                color = Color.DarkGray,
            )
            Image(
                painter = painterResource(id = R.drawable.task_assignment_map),
                contentDescription = "PNG display",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.padding(vertical = 8.dp))
            testData.forEach { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "${user.identifier}: ${user.role}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(top = 8.dp).padding(horizontal = 10.dp)
                    )
                    Text(
                        text = "Specialisation: ${user.specialisation}",
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                    )
                    Text(
                        text = "Location: ${user.currentLocation}",
                        modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                    )
                    Row (
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { navController.navigate(NavItem.ReceivedTaskScreen.route) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Assign Task")
                        }
                    }
                }
            }
        }
    }
}