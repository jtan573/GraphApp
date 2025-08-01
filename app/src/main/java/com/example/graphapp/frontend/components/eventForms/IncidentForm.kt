package com.example.graphapp.frontend.components.eventForms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.collections.set

@Composable
fun IncidentForm(
    fieldKeys: List<String>,
    eventInputMap: SnapshotStateMap<String, String>,
    onQuery: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFcce4eb)).padding(horizontal = 16.dp)
    ) {

        val inputKeys = listOf<String>("Incident") + fieldKeys

        Text(
            text = "Insert an Incident:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // All base fields
        inputKeys.forEach { key ->
            OutlinedTextField(
                value = eventInputMap[key] ?: "",
                onValueChange = { eventInputMap[key] = it },
                label = { Text(key, fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
//            Button(onClick = onSubmit) {
//                Text("Insert Event", fontSize = 12.sp)
//            }
            Button(onClick = onQuery) {
                Text("Query Event", fontSize = 12.sp)
            }
            Button(onClick = onCancel) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    }
}