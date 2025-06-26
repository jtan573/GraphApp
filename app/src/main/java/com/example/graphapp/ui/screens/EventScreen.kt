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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.graphapp.ui.components.EventForm
import com.example.graphapp.ui.components.GraphWebView
import com.example.graphapp.ui.viewmodels.GraphViewModel
import io.ktor.websocket.Frame.Text
import org.json.JSONObject
import kotlin.collections.set

@Composable
fun EventScreen(
    viewModel: GraphViewModel,
    navController: NavHostController
) {
    val filteredGraphData by viewModel.filteredGraphData.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    val fieldKeys = viewModel.getNodeTypes()
    val eventInputMap = remember(fieldKeys) {
        mutableStateMapOf<String, String>().apply {
            fieldKeys.forEach { putIfAbsent(it, "") }
        }
    }

    val events by viewModel.createdEvents.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Row (
            modifier = Modifier.fillMaxWidth(),
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
                Button(
                    onClick = { showForm = !showForm },
                    modifier = Modifier.padding(end = 3.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Text(if (showForm) "Hide" else "+ Event", fontSize = 12.sp)
                }
            }
        }

        AnimatedVisibility(visible = showForm) {
            EventForm(
                fieldKeys = fieldKeys,
                eventInputMap = eventInputMap,
                onSubmit = {
                    viewModel.createEvent(eventInputMap)
                    eventInputMap.clear()
                    fieldKeys.forEach { eventInputMap[it] = "" }
                    showForm = false
                },
                onCancel = { showForm = false }
            )
        }

        if (events.isEmpty()) {
            Text("No events added.", modifier = Modifier.padding(4.dp))
        } else {
            LazyColumn {
                items(events) { event ->
                    Text("Event: ${event.fields}", modifier = Modifier.padding(4.dp))
                }
            }
        }

        if (filteredGraphData != null) {
            GraphWebView(graphJson = filteredGraphData, modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
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