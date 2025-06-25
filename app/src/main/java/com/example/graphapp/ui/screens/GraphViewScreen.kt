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
import com.example.graphapp.ui.viewmodels.GraphViewModel
import org.json.JSONObject

@Composable
fun GraphViewScreen(viewModel: GraphViewModel) {
    val graphJson by viewModel.graphData.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    val fieldKeys = viewModel.getNodeTypes()
    val eventInputMap = remember(fieldKeys) {
        mutableStateMapOf<String, String>().apply {
            fieldKeys.forEach { putIfAbsent(it, "") }
        }
    }

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
                    Button(
                        onClick = {
                            viewModel.createEvent(eventInputMap)
                            eventInputMap.clear()
                            showForm = false
                            fieldKeys.forEach { eventInputMap[it] = "" }
                        }
                    ) {
                        Text("Insert Event", fontSize = 12.sp)
                    }
                    Button(onClick = { showForm = false }) {
                        Text("Cancel", fontSize = 12.sp)
                    }
                }
            }
        }

        if (graphJson != null) {
            AndroidView(
                factory = {
                    WebView(it).apply {
                        this.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        this.settings.javaScriptEnabled = true
                        this.isHorizontalScrollBarEnabled = true
                        this.isVerticalScrollBarEnabled = true
                        this.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                evaluateJavascript("loadGraph(${JSONObject.quote(graphJson)});") { result ->
                                    Log.d("GraphRelations", "JavaScript executed: $result")
                                }
                            }
                        }
                        loadUrl("file:///android_asset/graph.html")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                update = { webView ->
                    graphJson?.let {
                        Log.d("WebView", "Injecting graph: $it")
                        webView.evaluateJavascript("loadGraph(${JSONObject.quote(it)});") { result ->
                            Log.d("GraphWebView", "JS Result: $result")
                        }
                    }
                }
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