package com.example.graphapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EventForm(
    fieldKeys: List<String>,
    eventInputMap: SnapshotStateMap<String, String>,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Insert Event:")
        fieldKeys.forEach { key ->
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
            Button(onClick = onCancel) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    }
}
