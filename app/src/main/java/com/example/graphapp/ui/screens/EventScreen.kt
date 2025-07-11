package com.example.graphapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graphapp.data.schema.GraphSchema
import com.example.graphapp.ui.components.EventForm
import com.example.graphapp.ui.components.GraphWebView
import com.example.graphapp.ui.viewmodels.GraphViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.set

@Composable
fun EventScreen(
    viewModel: GraphViewModel
) {
    val filteredGraphData by viewModel.filteredGraphData.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    val fieldKeys = GraphSchema.SchemaPropertyNodes + GraphSchema.SchemaOtherNodes
    val eventInputMap = remember(fieldKeys) {
        mutableStateMapOf<String, String>().apply {
            fieldKeys.forEach { putIfAbsent(it, "") }
        }
    }

    // Dropdown state
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = GraphSchema.SchemaKeyNodes + GraphSchema.SchemaPropertyNodes + "All"

    val events by viewModel.createdEvents.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var activeButton by remember { mutableStateOf(ActiveButton.NONE) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row (
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Events",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top=64.dp, bottom=8.dp),
            )
            Box {
                Row (
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { showForm = !showForm },
                        enabled = !isLoading,
                        modifier = Modifier.padding(end = 3.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        if (isLoading && activeButton == ActiveButton.EVENT) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (showForm) "Hide" else "+ Event",
                                fontSize = 12.sp
                            )
                        }
                    }
                    Button(
                        onClick = { showFilterMenu = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        Text("Filter", fontSize = 12.sp)
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        filterOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = Color.Black) },
                                onClick = {
                                    selectedFilter = option
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = showForm) {
            EventForm(
                fieldKeys = fieldKeys,
                eventInputMap = eventInputMap,
                onSubmit = {
                    coroutineScope.launch {
                        withContext(Dispatchers.Main) {
                            isLoading = true
                            activeButton = ActiveButton.EVENT
                        }
                        viewModel.provideEventRec(eventInputMap)
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            activeButton = ActiveButton.NONE
                            eventInputMap.clear()
                            fieldKeys.forEach { eventInputMap[it] = "" }
                            showForm = false }
                    }
                },
                onCancel = { showForm = false }
            )
        }

        if (events.isEmpty()) {
            Text("No events added.", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn (
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(events) { event ->
                    Text(event, modifier = Modifier.padding(4.dp))
                }
            }
        }

        if (filteredGraphData != null) {
            GraphWebView(graphJson = filteredGraphData,
                selectedFilter = selectedFilter,
                modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
            )
        }
    }
}