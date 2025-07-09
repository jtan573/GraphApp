package com.example.graphapp.ui.screens

import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.graphapp.data.schema.GraphSchema
import com.example.graphapp.ui.components.EventForm
import com.example.graphapp.ui.components.GraphWebView
import com.example.graphapp.ui.viewmodels.GraphViewModel
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ActiveButton { NONE, EVENT, FILL, FIND }

@Composable
fun GraphViewScreen(
    viewModel: GraphViewModel,
    navController: NavHostController
) {
    val graphJson by viewModel.graphData.collectAsState()
    val selectedFilter = "All"

    var showForm by remember { mutableStateOf(false) }

    val fieldKeys = viewModel.getNodeTypes()
    val eventInputMap = remember(fieldKeys) {
        mutableStateMapOf<String, String>().apply {
            fieldKeys.forEach { putIfAbsent(it, "") }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var activeButton by remember { mutableStateOf(ActiveButton.NONE) }

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
                text = "GraphApp",
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
                        onClick = {
                            coroutineScope.launch(Dispatchers.Default) {
                                withContext(Dispatchers.Main) {
                                    isLoading = true
                                    activeButton = ActiveButton.FILL
                                }
                                viewModel.fillMissingLinks()
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    activeButton = ActiveButton.NONE
                                }
                            }},
                        enabled = !isLoading,
                        modifier = Modifier.padding(end = 3.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        if (isLoading  && activeButton == ActiveButton.FILL) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Fill", fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch(Dispatchers.Default) {
                                withContext(Dispatchers.Main) {
                                    isLoading = true
                                    activeButton = ActiveButton.FIND
                                }
                                viewModel.findGraphRelations()
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    activeButton = ActiveButton.NONE
                                }
                            }},
                        enabled = !isLoading,
                        modifier = Modifier.padding(end = 3.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
                    ) {
                        if (isLoading && activeButton == ActiveButton.FIND) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Find", fontSize = 12.sp)
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

        if (graphJson != null) {
            GraphWebView(graphJson = graphJson,
                selectedFilter = selectedFilter,
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
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
}