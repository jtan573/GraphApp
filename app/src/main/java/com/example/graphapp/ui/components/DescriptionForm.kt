package com.example.graphapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.util.TableInfo

@Composable
fun DescriptionForm(
    description: String,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit
) {

    var localDescription by remember { mutableStateOf(description) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = localDescription,
            onValueChange = {
                localDescription = it
                onDescriptionChange(it)
            },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 5
        )

        Button (
            onClick = { onSubmit() },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Query")
        }
    }
}
