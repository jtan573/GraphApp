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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graphapp.data.schema.ActiveButton
import com.example.graphapp.data.schema.UiEvent
import com.example.graphapp.ui.components.DescriptionForm
import com.example.graphapp.ui.components.GraphWebView
import com.example.graphapp.ui.viewmodels.GraphViewModel
import kotlinx.coroutines.launch
import kotlin.collections.set

@Composable
fun PersonnelScreen(
    viewModel: GraphViewModel
) {
    val graphJson by viewModel.userGraphData.collectAsState()
    val contactState by viewModel.relevantContactState.collectAsState()
    val selectedFilter = "All"

    val coroutineScope = rememberCoroutineScope()
    var showForm by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var descriptionText by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val uiEventFlow = viewModel.uiEvent

    LaunchedEffect(Unit) {
        uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                UiEvent.NavigateBack -> {
                    // TODO: handle navigation here
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Personnel Tracker",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 64.dp, bottom = 8.dp),
                )
                Button(
                    onClick = { showForm = !showForm },
                    enabled = !isLoading,
                    modifier = Modifier.padding(end = 3.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (showForm) "Hide" else "Search",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showForm) {
                DescriptionForm(
                    description = descriptionText,
                    onDescriptionChange = { descriptionText = it },
                    onSubmit = {
                        coroutineScope.launch {
                            isLoading = true
                            viewModel.findRelevantContacts(descriptionText)
                            isLoading = false
                            showForm = false
                        }
                    }
                )
            }

            if (contactState.isNotEmpty()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "User Query: $descriptionText",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    contactState.forEach { (identifier, name) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Active User: $identifier", style = MaterialTheme.typography.bodyLarge)
                                Text(text = name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            if (graphJson != null) {
                GraphWebView(
                    graphJson = graphJson,
                    selectedFilter = selectedFilter,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    htmlFileName = "userGraph.html"
                )
            } else {
                Text(
                    text = "Loading graph...",
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}