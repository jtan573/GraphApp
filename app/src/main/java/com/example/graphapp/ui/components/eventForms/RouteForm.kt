package com.example.graphapp.ui.components.eventForms

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graphapp.data.schema.GraphSchema
import kotlin.collections.set

@Composable
fun RouteForm(
    locationList: SnapshotStateList<String>,
//    onSubmit: () -> Unit,
    onQuery: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFcce4eb))
            .padding(horizontal = 16.dp).padding(bottom = 10.dp)
    ) {
        Text(
            text = "Insert Route Stations:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Location input fields
        locationList.forEachIndexed { index, location ->
            OutlinedTextField(
                value = location,
                onValueChange = { newValue -> locationList[index] = newValue },
                label = { Text("Location ${index + 1}", fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        // Add new location row
        Button(
            onClick = { locationList.add("") },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.Gray
            ),
            border = BorderStroke(1.dp, Color.LightGray)

        ) {
            Text("Add Another Patrol Stop", fontSize = 12.sp)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Button(onClick = onQuery) {
                Text("Query Event", fontSize = 12.sp)
            }
            Button(onClick = onCancel) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    }
}