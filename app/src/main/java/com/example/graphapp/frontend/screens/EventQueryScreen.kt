package com.example.graphapp.frontend.screens
//
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material3.Button
//import androidx.compose.material3.SnackbarHost
//import androidx.compose.material3.SnackbarHostState
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateMapOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.navigation.NavController
//import com.example.graphapp.data.schema.UiEvent
//import com.example.graphapp.data.schema.GraphSchema
//import com.example.graphapp.ui.components.EventForm
//import com.example.graphapp.ui.components.GraphWebView
//import com.example.graphapp.ui.viewmodels.GraphViewModel
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import kotlin.collections.set
//import com.example.graphapp.data.schema.ActiveButton
//import com.example.graphapp.ui.components.QueryResultCard
//
//@Composable
//fun EventQueryScreen(viewModel: GraphViewModel, navController: NavController) {
//    val filteredGraphData by viewModel.filteredGraphData.collectAsState()
//    val eventAdded by viewModel.createdEvent.collectAsState()
//    val queryResults by viewModel.queryResults.collectAsState()
//    val uiEventFlow = viewModel.uiEvent
//
//    val coroutineScope = rememberCoroutineScope()
//    var isLoading by remember { mutableStateOf(false) }
//    var activeButton by remember { mutableStateOf(ActiveButton.NONE) }
//    var showForm by remember { mutableStateOf(true) }
//    val snackbarHostState = remember { SnackbarHostState() }
//
//    val fieldKeys = GraphSchema.SchemaPropertyNodes + GraphSchema.SchemaOtherNodes
//    val eventInputMap = remember(fieldKeys) {
//        mutableStateMapOf<String, String>().apply {
//            fieldKeys.forEach { putIfAbsent(it, "") }
//        }
//    }
//
//    LaunchedEffect(Unit) {
//        uiEventFlow.collect { event ->
//            when (event) {
//                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
//                UiEvent.NavigateBack -> {}
//            }
//        }
//    }
//
//    Column(
//        modifier = Modifier.fillMaxSize()
//    ) {
//        Row(
//            modifier = Modifier.fillMaxWidth()
//                .padding(horizontal = 16.dp),
//            verticalAlignment = Alignment.Bottom,
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Text(
//                text = "Submit an Event Query",
//                fontSize = 24.sp,
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier.padding(top = 64.dp, bottom = 8.dp)
//            )
//            if (!showForm) {
//                Button(
//                    onClick = { showForm = true },
//                    modifier = Modifier.padding(end = 3.dp),
//                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
//                ) {
//                    Text("+ Event")
//                }
//            }
//        }
//
//        Box(modifier = Modifier.fillMaxSize()) {
//            Column(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .verticalScroll(rememberScrollState())
//            ) {
//                // Show form only if visible
//                AnimatedVisibility(visible = showForm) {
//                    EventForm(
//                        fieldKeys = fieldKeys,
//                        eventInputMap = eventInputMap,
//                        onSubmit = {
//                            coroutineScope.launch {
//                                withContext(Dispatchers.Main) {
//                                    isLoading = true
//                                    activeButton = ActiveButton.EVENT
//                                }
//                                viewModel.provideEventRecommendation(eventInputMap, false)
//                                withContext(Dispatchers.Main) {
//                                    isLoading = false
//                                    activeButton = ActiveButton.NONE
//                                    eventInputMap.clear()
//                                    fieldKeys.forEach { eventInputMap[it] = "" }
//                                    showForm = false
//                                }
//                            }
//                        },
//                        onQuery = {
//                            coroutineScope.launch {
//                                withContext(Dispatchers.Main) {
//                                    isLoading = true
//                                    activeButton = ActiveButton.EVENT
//                                }
////                            viewModel.provideEventRecommendation(eventInputMap, true)
//                                viewModel.findThreatAlertAndResponse(eventInputMap)
//                                withContext(Dispatchers.Main) {
//                                    isLoading = false
//                                    activeButton = ActiveButton.NONE
//                                    eventInputMap.clear()
//                                    fieldKeys.forEach { eventInputMap[it] = "" }
//                                    showForm = false //
//                                }
//                            }
//                        },
//                        onCancel = { showForm = false }
//                    )
//                }
//
//                queryResults?.let { QueryResultCard(eventAdded, it, navController) }
//
//                if (filteredGraphData != null) {
//                    GraphWebView(
//                        graphJson = filteredGraphData,
//                        selectedFilter = "All",
//                        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp)
//                    )
//                }
//            }
//
//            SnackbarHost(
//                hostState = snackbarHostState,
//                modifier = Modifier.align(Alignment.BottomCenter)
//            )
//        }
//    }
//
//}
//
