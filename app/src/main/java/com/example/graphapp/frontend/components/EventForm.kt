package com.example.graphapp.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graphapp.backend.dto.GraphSchema

@Composable
fun EventForm(
    fieldKeys: List<String>,
    eventInputMap: SnapshotStateMap<String, String>,
    onSubmit: () -> Unit,
    onQuery: () -> Unit,
    onCancel: () -> Unit
) {

    val eventTypes = GraphSchema.SchemaKeyNodes
    val selectedTab = remember { mutableStateOf(0) }

    // Keep event type in input map
    LaunchedEffect(selectedTab.value) {
        eventInputMap[eventTypes[selectedTab.value]] = ""
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp).background(Color(0xFFcce4eb))) {
        // Tabs
        TabRow(selectedTabIndex = selectedTab.value) {
            eventTypes.forEachIndexed { index, type ->
                Tab(
                    selected = selectedTab.value == index,
                    onClick = { selectedTab.value = index },
                    text = { Text(type) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val inputKeys = arrayOf(eventTypes[selectedTab.value]) + fieldKeys

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
            Button(onClick = onSubmit) {
                Text("Insert Event", fontSize = 12.sp)
            }
            Button(onClick = onQuery) {
                Text("Query Event", fontSize = 12.sp)
            }
            Button(onClick = onCancel) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    }
}
